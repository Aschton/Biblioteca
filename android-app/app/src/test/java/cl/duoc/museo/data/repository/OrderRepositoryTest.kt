package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.OrderDao
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserDao
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.remote.PedidosApi
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

class OrderRepositoryTest {

    private val dao = mockk<OrderDao>(relaxUnitFun = true)
    private val userDao = mockk<UserDao>()
    private val db = mockk<AppDatabase>()
    private val api = mockk<PedidosApi>()
    private val session = mockk<SessionManager>()
    private val catalog = mockk<CatalogRepository>(relaxUnitFun = true)
    private val notifications = mockk<NotificationRepository>(relaxUnitFun = true)
    private val userId = MutableStateFlow<Long?>(1L)
    private val user = MutableStateFlow<UserEntity?>(Fixtures.user(id = 1))
    private lateinit var repo: OrderRepository

    @Before fun setUp() {
        every { db.orderDao() } returns dao
        every { db.userDao() } returns userDao
        every { session.userId } returns userId
        coEvery { session.current() } answers { user.value }
        coEvery { userDao.findByEmail(any()) } returns Fixtures.user(id = 1)
        repo = OrderRepository(db, api, session, catalog, notifications)
    }

    @Test fun `historial propio y de todos`() = runTest {
        every { dao.observeByUser(1) } returns flowOf(listOf(Fixtures.order()))
        every { dao.observeAll() } returns flowOf(listOf(Fixtures.order(1), Fixtures.order(2)))

        assertEquals(1, repo.observeMine().first().size)
        assertEquals(2, repo.observeAll().first().size)

        userId.value = null
        assertTrue(repo.observeMine().first().isEmpty())
    }

    @Test fun `refresh del cliente trae solo sus pedidos y reenvia los pendientes`() = runTest {
        val offline = Fixtures.order(id = 100_500, synced = false)
        coEvery { dao.unsynced() } returns listOf(offline)
        coEvery { api.createOrder(any()) } returns Fixtures.orderDto(id = 50)
        coEvery { api.orders("user1@museo.cl") } returns listOf(Fixtures.orderDto(id = 1))

        assertTrue(repo.refresh())

        coVerify { dao.deleteById(100_500) }
        coVerify { dao.upsert(match { it.id == 50L && it.itemsSummary == offline.itemsSummary && it.synced }) }
        coVerify { dao.upsertAll(match { it.size == 1 && it[0].userId == 1L && it[0].itemsCount == 2 }) }
    }

    @Test fun `refresh del administrador trae todos`() = runTest {
        user.value = Fixtures.user(id = 1, role = Role.ADMIN)
        coEvery { dao.unsynced() } returns emptyList()
        coEvery { api.orders(null) } returns listOf(Fixtures.orderDto(1), Fixtures.orderDto(2, kind = null))

        assertTrue(repo.refresh())

        coVerify { dao.upsertAll(match { it.size == 2 && it[1].kind == "COMPRA" }) }
    }

    @Test fun `refresh sin sesion no hace nada y sin red falla`() = runTest {
        user.value = null
        assertTrue(repo.refresh())

        user.value = Fixtures.user(id = 1)
        coEvery { dao.unsynced() } returns emptyList()
        coEvery { api.orders(any()) } throws IOException()
        assertFalse(repo.refresh())
    }

    @Test fun `confirmar compra en linea descuenta stock, vacia carrito y notifica`() = runTest {
        coEvery { api.createOrder(any()) } returns Fixtures.orderDto(id = 7)
        val lines = listOf(Fixtures.cartLine(Fixtures.product(id = 1, price = 10.0), 2))

        val order = repo.placeOrder(lines, "Chile", "ESTANDAR", 5.0)

        assertEquals(7L, order.id)
        assertEquals("2x Producto 1", order.itemsSummary)
        coVerify { api.createOrder(match { it.items.size == 1 && it.items[0].quantity == 2 && it.userEmail == "user1@museo.cl" }) }
        coVerify { dao.upsert(order) }
        coVerify { catalog.decreaseStock(1, 2) }
        coVerify { catalog.clearCart() }
        coVerify(exactly = 2) { notifications.publish(any(), any(), "PEDIDO", any(), any(), "orders", any()) }
    }

    @Test fun `sin servidor la compra falla y no se toca el stock ni el carrito`() = runTest {
        coEvery { api.createOrder(any()) } throws IOException()
        val lines = listOf(Fixtures.cartLine(Fixtures.product(id = 1, price = 10.0), 3))

        val error = runCatching { repo.placeOrder(lines, "Perú", "EXPRESS", 22.4) }.exceptionOrNull()

        assertEquals(OrderRepository.NO_SERVER, error?.message)
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { catalog.decreaseStock(any(), any()) }
        coVerify(exactly = 0) { catalog.clearCart() }
        coVerify(exactly = 0) { notifications.publish(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test(expected = IllegalStateException::class)
    fun `sin sesion no se puede comprar`() = runTest {
        user.value = null
        repo.placeOrder(emptyList(), "Chile", "ESTANDAR", 0.0)
    }

    @Test fun `solicitud de informacion de curso`() = runTest {
        val curso = Fixtures.product(id = 2, category = "CURSO")
        coEvery { api.createOrder(match { it.kind == "INFO_CURSO" }) } returns Fixtures.orderDto(id = 8, kind = "INFO_CURSO")

        val online = repo.requestCourseInfo(curso)
        assertEquals(0.0, online.total, 0.0)
        assertEquals("Info: Producto 2", online.itemsSummary)

        coEvery { api.createOrder(any()) } throws IOException()
        val error = runCatching { repo.requestCourseInfo(curso) }.exceptionOrNull()
        assertEquals(OrderRepository.NO_SERVER, error?.message)

        coVerify(exactly = 1) { notifications.publish(any(), any(), "CURSO", Role.ADMIN.name, any(), "orders", any()) }
    }

    @Test fun `cambiar estado notifica al cliente`() = runTest {
        coEvery { api.updateStatus(1, any()) } returns Fixtures.orderDto(id = 1, status = "ENVIADO")
        val saved = slot<OrderEntity>()
        coEvery { dao.upsert(capture(saved)) } returns Unit

        repo.updateStatus(Fixtures.order(id = 1, userId = 3), OrderStatus.ENVIADO)
        assertEquals("ENVIADO", saved.captured.status)
        assertEquals(3L, saved.captured.userId)

        coEvery { api.updateStatus(any(), any()) } throws IOException()
        repo.updateStatus(Fixtures.order(id = 1), OrderStatus.ENTREGADO)
        assertEquals("ENTREGADO", saved.captured.status)

        repo.updateStatus(Fixtures.order(id = 2, synced = false), OrderStatus.CONFIRMADO)
        assertEquals("CONFIRMADO", saved.captured.status)
        coVerify(exactly = 2) { api.updateStatus(any(), any()) }
        coVerify(exactly = 3) { notifications.publish("Estado de tu pedido", any(), "PEDIDO", any(), any(), "orders", any()) }
    }

    @Test fun `eliminar solicitud`() = runTest {
        coEvery { api.deleteOrder(1) } returns Response.success(Unit)
        assertTrue(repo.delete(Fixtures.order(id = 1)))
        coVerify { dao.deleteById(1) }

        assertTrue(repo.delete(Fixtures.order(id = 2, synced = false)))
        coVerify { dao.deleteById(2) }
        coVerify(exactly = 0) { api.deleteOrder(2) }

        coEvery { api.deleteOrder(3) } returns Response.error(500, "".toResponseBody(null))
        assertFalse(repo.delete(Fixtures.order(id = 3)))
        coVerify(exactly = 0) { dao.deleteById(3) }
    }
}
