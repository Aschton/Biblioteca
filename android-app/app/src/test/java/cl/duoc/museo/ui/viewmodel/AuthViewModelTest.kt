package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val auth = mockk<AuthRepository>()

    @Test fun `login valida correo y contrasena antes de llamar al repositorio`() = runTest {
        val vm = AuthViewModel(auth)

        vm.login("malo", "x", Role.CLIENTE)
        assertEquals("Ingresa un correo válido", vm.state.value.error)
        vm.login("cliente@museo.cl", " ", Role.CLIENTE)
        assertEquals("Ingresa tu contraseña", vm.state.value.error)
        vm.clearError()
        assertNull(vm.state.value.error)

        coVerify(exactly = 0) { auth.login(any(), any(), any()) }
    }

    @Test fun `login exitoso y fallido`() = runTest {
        val vm = AuthViewModel(auth)
        coEvery { auth.login("cliente@museo.cl", "Cliente123", Role.CLIENTE) } returns Result.success(Fixtures.user())
        coEvery { auth.login("cliente@museo.cl", "mala", Role.CLIENTE) } returns Result.failure(IllegalArgumentException("Contraseña incorrecta"))

        vm.login("cliente@museo.cl", "mala", Role.CLIENTE)
        assertEquals("Contraseña incorrecta", vm.state.value.error)
        assertFalse(vm.state.value.loading)

        vm.login("cliente@museo.cl", "Cliente123", Role.CLIENTE)
        assertTrue(vm.state.value.loggedIn)
    }

    @Test fun `registro muestra errores por campo`() = runTest {
        val vm = AuthViewModel(auth)
        vm.updateForm { it.copy(name = "Al", email = "x", password = "123", confirm = "456") }

        vm.register()

        val f = vm.form.value
        assertNotNull(f.nameError)
        assertNotNull(f.emailError)
        assertNotNull(f.passwordError)
        assertNotNull(f.confirmError)
        coVerify(exactly = 0) { auth.register(any(), any(), any(), any(), any()) }
    }

    @Test fun `registro valido crea la cuenta`() = runTest {
        val vm = AuthViewModel(auth)
        coEvery { auth.register("Ana Pérez", "ana@museo.cl", "Clave123", "avatar_1", "Arte,Historia") } returns Result.success(Fixtures.user())
        vm.updateForm { it.copy(name = "Ana Pérez", email = "ana@museo.cl", password = "Clave123", confirm = "Clave123", interests = setOf("Arte", "Historia")) }

        vm.register()

        assertTrue(vm.state.value.loggedIn)
    }

    @Test fun `registro con correo repetido muestra el error`() = runTest {
        val vm = AuthViewModel(auth)
        coEvery { auth.register(any(), any(), any(), any(), any()) } returns Result.failure(IllegalArgumentException("Ese correo ya está registrado"))
        vm.updateForm { it.copy(name = "Ana Pérez", email = "ana@museo.cl", password = "Clave123", confirm = "Clave123") }

        vm.register()

        assertEquals("Ese correo ya está registrado", vm.state.value.error)
    }
}
