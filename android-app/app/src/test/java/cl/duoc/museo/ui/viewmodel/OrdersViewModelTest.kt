package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.data.repository.OrderStatus
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrdersViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<OrderRepository>(relaxUnitFun = true)
    private val session = mockk<SessionManager>()

    @Test fun `el cliente ve sus pedidos y puede filtrar por estado`() = runTest {
        every { session.currentUser } returns flowOf(Fixtures.user(role = Role.CLIENTE))
        every { repo.observeMine() } returns flowOf(listOf(Fixtures.order(1), Fixtures.order(2, status = "ENVIADO")))
        coEvery { repo.refresh() } returns true

        val vm = OrdersViewModel(repo, session)
        keepCollecting(vm.state)

        assertFalse(vm.state.value.isAdmin)
        assertEquals(2, vm.state.value.orders.size)
        assertEquals(true, vm.state.value.online)
        vm.setFilter(OrderStatus.ENVIADO)
        assertEquals(listOf(2L), vm.state.value.orders.map { it.id })
    }

    @Test fun `el administrador gestiona y elimina solicitudes`() = runTest {
        every { session.currentUser } returns flowOf(Fixtures.user(role = Role.ADMIN))
        every { repo.observeAll() } returns flowOf(listOf(Fixtures.order(1)))
        coEvery { repo.refresh() } returns false
        val vm = OrdersViewModel(repo, session)
        keepCollecting(vm.state)
        assertTrue(vm.state.value.isAdmin)

        val o = Fixtures.order(1)
        vm.updateStatus(o, OrderStatus.CONFIRMADO)
        coVerify { repo.updateStatus(o, OrderStatus.CONFIRMADO) }

        coEvery { repo.delete(o) } returns true
        vm.delete(o)
        assertEquals("Solicitud ${o.code} eliminada", vm.message.value)

        coEvery { repo.delete(o) } returns false
        vm.delete(o)
        assertEquals("No se pudo eliminar: el servidor no responde", vm.message.value)
        vm.clearMessage()
        assertNull(vm.message.value)
    }
}
