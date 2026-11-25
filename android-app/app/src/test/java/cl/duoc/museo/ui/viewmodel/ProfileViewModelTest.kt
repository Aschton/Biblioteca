package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val auth = mockk<AuthRepository>(relaxUnitFun = true)
    private val session = mockk<SessionManager>()
    private val user = MutableStateFlow<UserEntity?>(Fixtures.user())

    private fun vm(): ProfileViewModel {
        every { session.currentUser } returns user
        return ProfileViewModel(auth, session)
    }

    @Test fun `guardar datos valida nombre y correo`() = runTest {
        val vm = vm()
        keepCollecting(vm.user)

        assertFalse(vm.saveData("Al", "a@museo.cl", ""))
        assertEquals("El nombre es muy corto", vm.message.value)
        assertFalse(vm.saveData("Alicia", "malo", ""))
        assertEquals("Correo no válido", vm.message.value)

        assertTrue(vm.saveData(" Alicia ", "ALICIA@Museo.cl", " Calle 1 "))
        assertEquals("Cambios guardados", vm.message.value)
        coVerify { auth.update(match { it.name == "Alicia" && it.email == "alicia@museo.cl" && it.shippingAddress == "Calle 1" }) }

        vm.clearMessage()
        assertNull(vm.message.value)
    }

    @Test fun `cambio de contrasena`() = runTest {
        val vm = vm()
        keepCollecting(vm.user)

        vm.changePassword("Cliente123", "abc")
        assertTrue(vm.message.value!!.startsWith("La nueva contraseña"))

        coEvery { auth.changePassword(any(), "mala", "Nueva123") } returns Result.failure(IllegalArgumentException("La contraseña actual no coincide"))
        vm.changePassword("mala", "Nueva123")
        assertEquals("La contraseña actual no coincide", vm.message.value)

        coEvery { auth.changePassword(any(), "Cliente123", "Nueva123") } returns Result.success(Unit)
        vm.changePassword("Cliente123", "Nueva123")
        assertEquals("Contraseña actualizada", vm.message.value)
    }

    @Test fun `sin usuario no guarda nada y cerrar sesion`() = runTest {
        user.value = null
        val vm = vm()
        keepCollecting(vm.user)

        vm.save({ it.copy(name = "x") })
        vm.changePassword("a", "Nueva123")
        vm.logout()

        coVerify(exactly = 0) { auth.update(any()) }
        coVerify(exactly = 0) { auth.changePassword(any(), any(), any()) }
        verify { auth.logout() }
    }
}
