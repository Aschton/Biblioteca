package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.local.QrHistoryEntity
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.QrRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `agrupa favoritos, consultados y lecturas QR`() = runTest {
        val museums = mockk<MuseumRepository>(relaxUnitFun = true)
        val catalog = mockk<CatalogRepository>(relaxUnitFun = true)
        val qr = mockk<QrRepository>()
        val h = QrHistoryEntity(userId = 1, code = "PROD:1", itemType = "PRODUCT", itemId = 1, title = "x")
        every { museums.observeFavorites() } returns flowOf(listOf(Fixtures.museum(1)))
        every { museums.observeViewed() } returns flowOf(listOf(Fixtures.museum(1), Fixtures.museum(2)))
        every { catalog.observeFavoriteProducts() } returns flowOf(listOf(Fixtures.product()))
        every { qr.observeHistory() } returns flowOf(listOf(h))

        val vm = FavoritesViewModel(museums, catalog, qr)
        keepCollecting(vm.state)

        assertEquals(1, vm.state.value.museums.size)
        assertEquals(2, vm.state.value.viewed.size)
        assertEquals(1, vm.state.value.products.size)
        assertEquals(listOf(h), vm.state.value.qrHistory)

        vm.removeMuseum(1)
        vm.removeProduct(1)
        coVerify { museums.setFavorite(1, false) }
        coVerify { catalog.setFavorite(1, false) }
    }
}
