package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.HistoryDao
import cl.duoc.museo.data.local.MuseumDao
import cl.duoc.museo.data.local.ProductDao
import cl.duoc.museo.data.local.QrHistoryEntity
import cl.duoc.museo.data.remote.CatalogoApi
import cl.duoc.museo.data.remote.MuseosApi
import cl.duoc.museo.data.remote.QrResolveDto
import cl.duoc.museo.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class QrRepositoryTest {

    private val museumDao = mockk<MuseumDao>()
    private val productDao = mockk<ProductDao>()
    private val historyDao = mockk<HistoryDao>(relaxUnitFun = true)
    private val db = mockk<AppDatabase>()
    private val museosApi = mockk<MuseosApi>()
    private val catalogoApi = mockk<CatalogoApi>()
    private val session = mockk<SessionManager>()
    private val userId = MutableStateFlow<Long?>(1L)
    private lateinit var repo: QrRepository

    @Before fun setUp() {
        every { db.museumDao() } returns museumDao
        every { db.productDao() } returns productDao
        every { db.historyDao() } returns historyDao
        every { session.userId } returns userId
        every { session.currentId() } answers { userId.value }
        coEvery { museumDao.getByQr(any()) } returns null
        coEvery { productDao.getByQr(any()) } returns null
        repo = QrRepository(db, museosApi, catalogoApi, session)
    }

    @Test fun `rechaza codigos que no son de la app`() = runTest {
        val r = repo.resolve("https://otra-cosa.com")
        assertEquals("El código no pertenece a Museo de los Museos", r.exceptionOrNull()?.message)
    }

    @Test fun `resuelve museo desde la cache y guarda historial`() = runTest {
        coEvery { museumDao.getByQr("MUSEO:1") } returns Fixtures.museum(id = 1)

        val r = repo.resolve("  MUSEO:1 ").getOrThrow()

        assertEquals("MUSEUM", r.type)
        assertEquals("museum/1", r.route)
        assertEquals("Museo virtual · Chile", r.label)
        coVerify { historyDao.upsertQr(match<QrHistoryEntity> { it.userId == 1L && it.code == "MUSEO:1" && it.itemId == 1L }) }
        coVerify(exactly = 0) { museosApi.resolveQr(any()) }
    }

    @Test fun `etiquetas de productos segun categoria`() = runTest {
        val esperado = mapOf("CURSO" to "Curso", "LIBRO" to "Publicación", "MATERIAL" to "Material", "ARTICULO" to "Producto")
        esperado.forEach { (cat, label) ->
            coEvery { productDao.getByQr("PROD:4") } returns Fixtures.product(id = 4, category = cat)
            val r = repo.resolve("PROD:4").getOrThrow()
            assertEquals(label, r.label)
            assertEquals("product/4", r.route)
        }
    }

    @Test fun `si no esta en cache consulta el microservicio correcto`() = runTest {
        coEvery { museosApi.resolveQr("MUSEO:50") } returns QrResolveDto("MUSEO:50", "MUSEUM", 50, "Museo nuevo", "desc")
        coEvery { catalogoApi.resolveQr("PROD:60") } returns QrResolveDto("PROD:60", "PRODUCT", 60, "Libro nuevo", "desc")

        assertEquals("Museo virtual", repo.resolve("MUSEO:50").getOrThrow().label)
        assertEquals("Producto", repo.resolve("PROD:60").getOrThrow().label)
        coVerify(exactly = 0) { catalogoApi.resolveQr("MUSEO:50") }
        coVerify(exactly = 0) { museosApi.resolveQr("PROD:60") }
    }

    @Test fun `codigo desconocido sin sesion no guarda historial`() = runTest {
        userId.value = null
        coEvery { museosApi.resolveQr(any()) } throws IOException()
        coEvery { catalogoApi.resolveQr("PROD:2") } returns QrResolveDto("PROD:2", "PRODUCT", 2, "x", "y")

        assertTrue(repo.resolve("MUSEO:99").exceptionOrNull()!!.message!!.startsWith("Código desconocido"))
        assertTrue(repo.resolve("PROD:2").isSuccess)
        coVerify(exactly = 0) { historyDao.upsertQr(any()) }
    }

    @Test fun `historial por usuario`() = runTest {
        val h = QrHistoryEntity(userId = 1, code = "MUSEO:1", itemType = "MUSEUM", itemId = 1, title = "Museo 1")
        every { historyDao.observeQr(1) } returns flowOf(listOf(h))

        assertEquals(listOf(h), repo.observeHistory().first())
        userId.value = null
        assertTrue(repo.observeHistory().first().isEmpty())
    }
}
