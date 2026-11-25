package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.repository.Availability
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProductDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<CatalogRepository>(relaxUnitFun = true)
    private val orders = mockk<OrderRepository>()
    private val product = MutableStateFlow<ProductEntity?>(Fixtures.product(id = 1, stock = 3))
    private val favorite = MutableStateFlow(false)

    private fun vm(): ProductDetailViewModel {
        every { repo.observeProduct(1) } returns product
        every { repo.observeIsFavorite(1) } returns favorite
        return ProductDetailViewModel(SavedStateHandle(mapOf("id" to 1L)), repo, orders)
    }

    @Test fun `cantidad entre 1 y el stock`() = runTest {
        val vm = vm()
        keepCollecting(vm.product)

        vm.dec()
        assertEquals(1, vm.qty.value)
        repeat(5) { vm.inc() }
        assertEquals(3, vm.qty.value)
    }

    @Test fun `agregar al carrito valida disponibilidad`() = runTest {
        val vm = vm()
        coEvery { repo.checkAvailability(1, 1) } returns Availability(true, 3, true)

        vm.addToCart()
        assertEquals(ProductEvent.Added(1), vm.event.value)
        coVerify { repo.addToCart(1, 1) }
        assertFalse(vm.busy.value)

        coEvery { repo.checkAvailability(1, 1) } returns Availability(false, 0, true)
        vm.addToCart()
        assertEquals(ProductEvent.Error("Sin disponibilidad suficiente (stock: 0)"), vm.event.value)

        vm.consumeEvent()
        assertNull(vm.event.value)
    }

    @Test fun `favorito y solicitud de informacion`() = runTest {
        val vm = vm()
        keepCollecting(vm.product)
        keepCollecting(vm.isFavorite)
        coEvery { orders.requestCourseInfo(any()) } returns Fixtures.order(kind = "INFO_CURSO")

        vm.toggleFavorite()
        coVerify { repo.setFavorite(1, true) }

        vm.requestInfo()
        assertEquals(ProductEvent.InfoRequested, vm.event.value)
        coVerify { orders.requestCourseInfo(match { it.id == 1L }) }
    }

    @Test fun `solicitud de informacion sin servidor muestra error`() = runTest {
        val vm = vm()
        keepCollecting(vm.product)
        coEvery { orders.requestCourseInfo(any()) } throws IllegalStateException("No se pudo conectar con el servidor")

        vm.requestInfo()

        assertEquals(ProductEvent.Error("No se pudo conectar con el servidor"), vm.event.value)
    }

    @Test fun `sin producto cargado no se solicita informacion`() = runTest {
        product.value = null
        val vm = vm()

        vm.requestInfo()

        coVerify(exactly = 0) { orders.requestCourseInfo(any()) }
    }
}
