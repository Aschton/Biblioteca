package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.NotificationDao
import cl.duoc.museo.data.local.NotificationReadEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserDao
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.remote.MuseosApi
import cl.duoc.museo.data.remote.NoticeDto
import cl.duoc.museo.notifications.NotificationHelper
import cl.duoc.museo.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class NotificationRepositoryTest {

    private val dao = mockk<NotificationDao>(relaxUnitFun = true)
    private val userDao = mockk<UserDao>()
    private val db = mockk<AppDatabase>()
    private val api = mockk<MuseosApi>()
    private val session = mockk<SessionManager>()
    private val helper = mockk<NotificationHelper>(relaxUnitFun = true)
    private val user = MutableStateFlow<UserEntity?>(Fixtures.user(id = 1, role = Role.CLIENTE))
    private lateinit var repo: NotificationRepository

    @Before fun setUp() {
        every { db.notificationDao() } returns dao
        every { db.userDao() } returns userDao
        every { session.currentUser } returns user
        every { session.currentId() } answers { user.value?.id }
        coEvery { session.current() } answers { user.value }
        repo = NotificationRepository(db, api, session, helper)
    }

    @Test fun `avisos y no leidos del usuario activo`() = runTest {
        every { dao.observeFor(1, "CLIENTE") } returns flowOf(listOf(Fixtures.notification()))
        every { dao.observeUnread(1, "CLIENTE") } returns flowOf(4)

        assertEquals(1, repo.observeMine().first().size)
        assertEquals(4, repo.observeUnread().first())

        user.value = null
        assertTrue(repo.observeMine().first().isEmpty())
        assertEquals(0, repo.observeUnread().first())
    }

    @Test fun `marcar como leidos`() = runTest {
        repo.markRead(5)
        repo.markAllRead()
        coVerify { dao.markRead(NotificationReadEntity(1, 5)) }
        coVerify { dao.markAllRead(1, "CLIENTE") }

        user.value = null
        repo.markRead(6)
        repo.markAllRead()
        coVerify(exactly = 1) { dao.markRead(any()) }
        coVerify(exactly = 1) { dao.markAllRead(any(), any()) }
    }

    @Test fun `publicar guarda, envia al microservicio y muestra la notificacion`() = runTest {
        coEvery { dao.insert(any()) } returns 10L
        coEvery { api.createNotice(any()) } returns NoticeDto(id = 1, title = "t", body = "b", category = "EVENTO")

        repo.publish("Hola", "Mundo", "EVENTO", destination = "home", sendToBackend = true)

        coVerify { dao.insert(match { it.title == "Hola" && it.targetRole == "ALL" }) }
        coVerify { api.createNotice(match { it.title == "Hola" && it.destination == "home" }) }
        verify { helper.show(10L, "Hola", "Mundo", "home") }
    }

    @Test fun `no muestra la notificacion si el destinatario las desactivo`() = runTest {
        coEvery { dao.insert(any()) } returns 11L
        coEvery { userDao.getById(2) } returns Fixtures.user(id = 2, notifications = false)

        repo.publish("Pedido", "Listo", "PEDIDO", targetUserId = 2)

        coVerify(exactly = 0) { api.createNotice(any()) }
        verify(exactly = 0) { helper.show(any(), any(), any(), any()) }
    }

    @Test fun `refresh agrega solo avisos nuevos`() = runTest {
        coEvery { api.notices() } returns listOf(
            NoticeDto(title = "Nuevo", body = "b", category = "EVENTO", createdAt = 100L),
            NoticeDto(title = "Repetido", body = "b", category = "EVENTO", createdAt = 200L),
            NoticeDto(title = "Sin fecha", body = "b", category = "EVENTO", createdAt = null)
        )
        coEvery { dao.exists("Nuevo", 100L) } returns false
        coEvery { dao.exists("Repetido", 200L) } returns true
        coEvery { dao.insert(any()) } returns 1L

        assertTrue(repo.refresh())

        coVerify(exactly = 1) { dao.insert(match { it.title == "Nuevo" && it.createdAt == 100L }) }
    }

    @Test fun `refresh sin conexion`() = runTest {
        coEvery { api.notices() } throws IOException()
        assertFalse(repo.refresh())
    }
}
