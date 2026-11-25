package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.repository.NotificationRepository
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<NotificationRepository>(relaxUnitFun = true)

    @Test fun `filtra por categoria y marca leidas`() = runTest {
        every { repo.observeMine() } returns flowOf(listOf(Fixtures.notification(1, "CURSO"), Fixtures.notification(2, "PEDIDO")))
        coEvery { repo.refresh() } returns true

        val vm = NotificationsViewModel(repo)
        keepCollecting(vm.state)
        assertEquals(2, vm.state.value.items.size)

        vm.setFilter("PEDIDO")
        assertEquals(listOf(2L), vm.state.value.items.map { it.notification.id })

        vm.markRead(2)
        vm.markAllRead()
        coVerify { repo.markRead(2) }
        coVerify { repo.markAllRead() }
        coVerify { repo.refresh() }
    }

    @Test fun `aviso de prueba abre la pantalla segun la categoria`() = runTest {
        every { repo.observeMine() } returns flowOf(emptyList())
        coEvery { repo.refresh() } returns true
        val vm = NotificationsViewModel(repo)

        val destinos = mapOf("CURSO" to "catalog", "EVENTO" to "catalog", "MUSEO" to "map", "PEDIDO" to "orders", "OTRA" to "notifications")
        destinos.forEach { (cat, dest) ->
            vm.sendTest("Título", "Cuerpo", cat, "ALL")
            coVerify { repo.publish("Título", "Cuerpo", cat, "ALL", null, dest, true) }
        }
        assertTrue(vm.sent.value)
        vm.consumeSent()
        assertFalse(vm.sent.value)
    }
}
