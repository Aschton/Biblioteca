package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserDao
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.util.Security
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private val userDao = mockk<UserDao>(relaxUnitFun = true)
    private val db = mockk<AppDatabase>()
    private val session = mockk<SessionManager>(relaxUnitFun = true)
    private lateinit var repo: AuthRepository

    @Before fun setUp() {
        every { db.userDao() } returns userDao
        every { session.currentUser } returns flowOf(null)
        repo = AuthRepository(db, session)
    }

    @Test fun `login correcto inicia sesion`() = runTest {
        coEvery { userDao.findByEmail("user1@museo.cl") } returns Fixtures.user()

        val r = repo.login(" user1@museo.cl ", "Cliente123", Role.CLIENTE)

        assertTrue(r.isSuccess)
        verify { session.login(1L) }
    }

    @Test fun `login falla con correo inexistente, clave incorrecta, otro rol o cuenta inactiva`() = runTest {
        coEvery { userDao.findByEmail("nadie@museo.cl") } returns null
        assertEquals("No existe una cuenta con ese correo", repo.login("nadie@museo.cl", "x", Role.CLIENTE).exceptionOrNull()?.message)

        coEvery { userDao.findByEmail("user1@museo.cl") } returns Fixtures.user()
        assertEquals("Contraseña incorrecta", repo.login("user1@museo.cl", "otra", Role.CLIENTE).exceptionOrNull()?.message)
        assertTrue(repo.login("user1@museo.cl", "Cliente123", Role.ADMIN).exceptionOrNull()!!.message!!.contains("Administrador"))

        coEvery { userDao.findByEmail("user1@museo.cl") } returns Fixtures.user(active = false)
        assertEquals("La cuenta está desactivada", repo.login("user1@museo.cl", "Cliente123", Role.CLIENTE).exceptionOrNull()?.message)

        verify(exactly = 0) { session.login(any()) }
    }

    @Test fun `registro crea cliente con hash y abre sesion`() = runTest {
        coEvery { userDao.findByEmail(any()) } returns null
        val saved = slot<UserEntity>()
        coEvery { userDao.insert(capture(saved)) } returns 7L

        val r = repo.register(" Ana Pérez ", "ANA@museo.cl", "Clave123", "avatar_2", "Arte")

        assertEquals(7L, r.getOrThrow().id)
        assertEquals("ana@museo.cl", saved.captured.email)
        assertEquals("Ana Pérez", saved.captured.name)
        assertEquals(Role.CLIENTE.name, saved.captured.role)
        assertEquals(Security.sha256("Clave123"), saved.captured.passwordHash)
        verify { session.login(7L) }
    }

    @Test fun `no permite correos duplicados`() = runTest {
        coEvery { userDao.findByEmail("user1@museo.cl") } returns Fixtures.user()

        val r = repo.createUser("Otro", "user1@museo.cl", "Clave123", Role.INVESTIGADOR)

        assertEquals("Ese correo ya está registrado", r.exceptionOrNull()?.message)
    }

    @Test fun `cambio de contrasena`() = runTest {
        val u = Fixtures.user()
        assertTrue(repo.changePassword(u, "mala", "Nueva123").isFailure)

        assertTrue(repo.changePassword(u, "Cliente123", "Nueva123").isSuccess)
        coVerify { userDao.update(u.copy(passwordHash = Security.sha256("Nueva123"))) }
    }

    @Test fun `administracion de cuentas y cierre de sesion`() = runTest {
        val u = Fixtures.user()
        every { userDao.observeAll() } returns flowOf(listOf(u))

        assertEquals(listOf(u), repo.observeAllUsers().first())
        repo.update(u)
        repo.setActive(u, false)
        repo.deleteUser(u)
        repo.logout()

        coVerify { userDao.update(u) }
        coVerify { userDao.update(u.copy(active = false)) }
        coVerify { userDao.delete(u) }
        verify { session.logout() }
        assertEquals(null, repo.currentUser.first())
    }
}
