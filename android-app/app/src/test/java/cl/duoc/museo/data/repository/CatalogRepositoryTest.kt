package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.CartDao
import cl.duoc.museo.data.local.CartItemEntity
import cl.duoc.museo.data.local.FavoriteDao
import cl.duoc.museo.data.local.ProductDao
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.remote.AvailabilityDto
import cl.duoc.museo.data.remote.CatalogoApi
import cl.duoc.museo.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class CatalogRepositoryTest {

    private val productDao = mockk<ProductDao>(relaxUnitFun = true)
    private val favoriteDao = mockk<FavoriteDao>(relaxUnitFun = true)
    private val cartDao = mockk<CartDao>(relaxUnitFun = true)
    private val db = mockk<AppDatabase>()
    private val api = mockk<CatalogoApi>()
    private val session = mockk<SessionManager>()
    private val userId = MutableStateFlow<Long?>(1L)
    private lateinit var repo: CatalogRepository

    @Before fun setUp() {
        every { db.productDao() } returns productDao
        every { db.favoriteDao() } returns favoriteDao
        every { db.cartDao() } returns cartDao
        every { session.userId } returns userId
        every { session.currentId() } answers { userId.value }
        repo = CatalogRepository(db, api, session)
    }

    @Test fun `refresh guarda el catalogo o informa error`() = runTest {
        coEvery { api.products(any(), any()) } returns listOf(Fixtures.productDto(1), Fixtures.productDto(2))
        assertTrue(repo.refresh())
        coVerify { productDao.upsertAll(match { it.size == 2 }) }

        coEvery { api.products(any(), any()) } throws IOException()
        assertFalse(repo.refresh())
    }

    @Test fun `disponibilidad en linea`() = runTest {
        coEvery { api.availability(1, 2) } returns AvailabilityDto(1, true, 8)

        val a = repo.checkAvailability(1, 2)

        assertTrue(a.available)
        assertEquals(8, a.stock)
        assertTrue(a.checkedOnline)
    }

    @Test fun `disponibilidad sin conexion usa el stock local`() = runTest {
        coEvery { api.availability(any(), any()) } throws IOException()
        coEvery { productDao.getById(1) } returns Fixtures.product(id = 1, stock = 3)
        coEvery { productDao.getById(2) } returns null

        assertTrue(repo.checkAvailability(1, 3).available)
        assertFalse(repo.checkAvailability(1, 4).available)
        val missing = repo.checkAvailability(2, 1)
        assertFalse(missing.available)
        assertEquals(0, missing.stock)
        assertFalse(missing.checkedOnline)
    }

    @Test fun `guardar producto nuevo y existente en linea`() = runTest {
        coEvery { api.createProduct(any()) } returns Fixtures.productDto(10)
        coEvery { api.updateProduct(2, any()) } returns Fixtures.productDto(2)

        assertTrue(repo.save(Fixtures.product(id = 0).copy(imageUrl = "/foto.jpg"), isNew = true))
        assertTrue(repo.save(Fixtures.product(id = 2), isNew = false))

        coVerify { api.createProduct(match { it.id == null }) }
        coVerify { productDao.upsert(match { it.id == 10L && it.imageUrl == "/foto.jpg" }) }
        coVerify { productDao.upsert(match { it.id == 2L }) }
    }

    @Test fun `sin conexion se guarda localmente`() = runTest {
        coEvery { api.createProduct(any()) } throws IOException()
        coEvery { api.updateProduct(any(), any()) } throws IOException()
        val saved = slot<ProductEntity>()
        coEvery { productDao.upsert(capture(saved)) } returns Unit

        assertFalse(repo.save(Fixtures.product(id = 0), isNew = true))
        assertTrue(saved.captured.id > 0)
        assertEquals("PROD:${saved.captured.id}", saved.captured.qrCode)

        val existente = Fixtures.product(id = 2)
        assertFalse(repo.save(existente, isNew = false))
        assertEquals(existente, saved.captured)
    }

    @Test fun `desactivar llama al endpoint y guarda`() = runTest {
        coEvery { api.deactivateProduct(2) } returns Fixtures.productDto(2).copy(active = false)
        coEvery { api.updateProduct(2, any()) } returns Fixtures.productDto(2).copy(active = false)

        assertTrue(repo.setActive(Fixtures.product(id = 2), false))
        coVerify { api.deactivateProduct(2) }

        coEvery { api.updateProduct(2, any()) } returns Fixtures.productDto(2)
        assertTrue(repo.setActive(Fixtures.product(id = 2, active = false), true))
        coVerify(exactly = 1) { api.deactivateProduct(2) }
    }

    @Test fun `eliminar producto`() = runTest {
        coEvery { api.deleteProduct(2) } returns Response.success(Unit)
        assertTrue(repo.delete(Fixtures.product(id = 2)))
        coVerify { productDao.deleteById(2) }

        coEvery { api.deleteProduct(3) } returns Response.error(404, "".toResponseBody(null))
        assertFalse(repo.delete(Fixtures.product(id = 3)))
        coVerify(exactly = 0) { productDao.deleteById(3) }
    }

    @Test fun `descontar stock nunca baja de cero`() = runTest {
        coEvery { productDao.getById(1) } returns Fixtures.product(id = 1, stock = 2)
        coEvery { productDao.getById(99) } returns null

        repo.decreaseStock(1, 5)
        repo.decreaseStock(99, 1)

        coVerify { productDao.upsert(match { it.id == 1L && it.stock == 0 }) }
        coVerify(exactly = 1) { productDao.upsert(any()) }
    }

    @Test fun `lecturas del catalogo`() = runTest {
        val p = Fixtures.product()
        every { productDao.observeAll(false) } returns flowOf(listOf(p))
        every { productDao.observeById(1) } returns flowOf(p)
        coEvery { productDao.getById(1) } returns p

        assertEquals(listOf(p), repo.observeProducts(false).first())
        assertEquals(p, repo.observeProduct(1).first())
        assertEquals(p, repo.getProduct(1))
    }

    @Test fun `favoritos de productos`() = runTest {
        val p = Fixtures.product()
        every { favoriteDao.observeProducts(1) } returns flowOf(listOf(p))
        every { favoriteDao.observeIsFavorite(1, CatalogRepository.TYPE, 1) } returns flowOf(true)

        assertEquals(listOf(p), repo.observeFavoriteProducts().first())
        assertTrue(repo.observeIsFavorite(1).first())
        repo.setFavorite(1, true)
        repo.setFavorite(1, false)

        coVerify { favoriteDao.insert(match { it.itemType == "PRODUCT" && it.itemId == 1L }) }
        coVerify { favoriteDao.delete(1, "PRODUCT", 1) }
    }

    @Test fun `carrito por usuario`() = runTest {
        every { cartDao.observeCart(1) } returns flowOf(listOf(Fixtures.cartLine()))
        every { cartDao.observeCount(1) } returns flowOf(3)
        coEvery { cartDao.get(1, 5) } returns CartItemEntity(1, 5, 2)
        coEvery { cartDao.get(1, 6) } returns null

        assertEquals(1, repo.observeCart().first().size)
        assertEquals(3, repo.observeCartCount().first())

        repo.addToCart(5, 1)
        repo.addToCart(6, 4)
        repo.setQuantity(5, 0)
        repo.setQuantity(6, 2)
        repo.clearCart()

        coVerify { cartDao.upsert(CartItemEntity(1, 5, 3)) }
        coVerify { cartDao.upsert(CartItemEntity(1, 6, 4)) }
        coVerify { cartDao.delete(1, 5) }
        coVerify { cartDao.upsert(CartItemEntity(1, 6, 2)) }
        coVerify { cartDao.clear(1) }
    }

    @Test fun `sin sesion el carrito y favoritos quedan vacios`() = runTest {
        userId.value = null

        assertTrue(repo.observeCart().first().isEmpty())
        assertEquals(0, repo.observeCartCount().first())
        assertTrue(repo.observeFavoriteProducts().first().isEmpty())
        assertFalse(repo.observeIsFavorite(1).first())
        repo.addToCart(1, 1)
        repo.setQuantity(1, 1)
        repo.setFavorite(1, true)
        repo.clearCart()

        coVerify(exactly = 0) { cartDao.upsert(any()) }
        coVerify(exactly = 0) { cartDao.clear(any()) }
        coVerify(exactly = 0) { favoriteDao.insert(any()) }
    }
}
