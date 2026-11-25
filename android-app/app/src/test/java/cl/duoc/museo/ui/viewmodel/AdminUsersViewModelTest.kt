package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AdminUsersViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val auth = mockk<AuthRepository>(relaxUnitFun = true)

    @Test fun `crear cuenta valida datos`() = runTest {
        every { auth.observeAllUsers() } returns flowOf(listOf(Fixtures.user()))
        val vm = AdminUsersViewModel(auth)
        keepCollecting(vm.users)
        assertEquals(1, vm.users.value.size)

        vm.create(" ", "a@museo.cl", "Clave123", Role.INVESTIGADOR)
        assertEquals("Ingresa un nombre", vm.message.value)
        vm.create("Inv", "malo", "Clave123", Role.INVESTIGADOR)
        assertEquals("Correo no válido", vm.message.value)
        vm.create("Inv", "inv@museo.cl", "123", Role.INVESTIGADOR)
        assertEquals("Contraseña: mín. 6, letras y números", vm.message.value)

        coEvery { auth.createUser("Inv", "inv@museo.cl", "Clave123", Role.INVESTIGADOR) } returns Result.success(Fixtures.user(id = 5))
        vm.create("Inv", "inv@museo.cl", "Clave123", Role.INVESTIGADOR)
        assertEquals("Cuenta creada", vm.message.value)

        coEvery { auth.createUser("Inv", "dup@museo.cl", "Clave123", Role.INVESTIGADOR) } returns Result.failure(IllegalArgumentException("Ese correo ya está registrado"))
        vm.create("Inv", "dup@museo.cl", "Clave123", Role.INVESTIGADOR)
        assertEquals("Ese correo ya está registrado", vm.message.value)

        vm.clearMessage()
        assertNull(vm.message.value)
    }

    @Test fun `activar, desactivar y eliminar`() = runTest {
        every { auth.observeAllUsers() } returns flowOf(emptyList())
        val vm = AdminUsersViewModel(auth)
        val u = Fixtures.user(active = true)

        vm.toggle(u)
        vm.delete(u)

        coVerify { auth.setActive(u, false) }
        coVerify { auth.deleteUser(u) }
    }
}
