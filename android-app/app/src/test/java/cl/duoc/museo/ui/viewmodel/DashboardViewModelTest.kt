package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.data.repository.OrderRepository
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
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `resume museos destacados, pendientes y pedidos`() = runTest {
        val museums = mockk<MuseumRepository>()
        val catalog = mockk<CatalogRepository>()
        val orders = mockk<OrderRepository>()
        val notifications = mockk<NotificationRepository>()
        val list = listOf(
            Fixtures.museum(id = 1, rating = 4.1),
            Fixtures.museum(id = 2, rating = 4.9),
            Fixtures.museum(id = 3, published = false),
            Fixtures.museum(id = 4, pendingSync = true, rating = 3.0)
        )
        every { museums.observeMuseums(true) } returns flowOf(list)
        every { catalog.observeProducts(false) } returns flowOf((1L..8L).map { Fixtures.product(id = it) })
        every { orders.observeAll() } returns flowOf(listOf(Fixtures.order(1), Fixtures.order(2, status = "ENVIADO")))
        coEvery { museums.refresh() } returns true
        coEvery { catalog.refresh() } returns true
        coEvery { orders.refresh() } returns true
        coEvery { notifications.refresh() } returns true

        val vm = DashboardViewModel(museums, catalog, orders, notifications)
        keepCollecting(vm.state)
        val s = vm.state.value

        assertEquals(listOf(2L, 1L, 4L), s.featuredMuseums.map { it.id })
        assertEquals(6, s.products.size)
        assertEquals(1, s.pendingOrders.size)
        assertEquals(listOf(3L, 4L), s.unpublishedMuseums.map { it.id })
        assertEquals(4, s.totalMuseums)
        assertEquals(true, s.online)
        coVerify { notifications.refresh() }
    }
}
