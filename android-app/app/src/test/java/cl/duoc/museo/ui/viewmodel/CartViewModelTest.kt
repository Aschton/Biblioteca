package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.local.CartLine
import cl.duoc.museo.data.repository.Availability
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import cl.duoc.museo.util.ShippingCalculator.ShippingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val catalog = mockk<CatalogRepository>(relaxUnitFun = true)
    private val orders = mockk<OrderRepository>()
    private val lines = MutableStateFlow(
        listOf(
            CartLine(Fixtures.product(id = 1, category = "LIBRO", price = 10.0), 2),
            CartLine(Fixtures.product(id = 2, category = "CURSO", price = 50.0), 1)
        )
    )

    private fun vm(): CartViewModel {
        every { catalog.observeCart() } returns lines
        return CartViewModel(catalog, orders)
    }

    @Test fun `calcula subtotal y envio, los cursos no pagan envio`() = runTest {
        val vm = vm()
        keepCollecting(vm.state)

        val s = vm.state.value
        assertEquals(70.0, s.subtotal, 0.001)
        assertEquals(5.0, s.shipping, 0.001) // Chile estándar, 2 unidades físicas
        assertEquals(75.0, s.total, 0.001)
        assertFalse(s.onlyDigital)

        vm.setCountry("Perú")
        vm.setType(ShippingType.RETIRO)
        assertEquals("El retiro en sede solo está disponible en Chile", vm.state.value.shippingError)
        assertEquals(0.0, vm.state.value.shipping, 0.001)

        lines.value = listOf(CartLine(Fixtures.product(id = 2, category = "CURSO"), 1))
        assertTrue(vm.state.value.onlyDigital)
    }

    @Test fun `checkout con error de envio o sin stock`() = runTest {
        val vm = vm()
        keepCollecting(vm.state)

        vm.setCountry("Perú")
        vm.setType(ShippingType.RETIRO)
        vm.checkout()
        assertEquals(CartEvent.Error("El retiro en sede solo está disponible en Chile"), vm.event.value)

        vm.setCountry("Chile")
        coEvery { catalog.checkAvailability(1, 2) } returns Availability(false, 1, true)
        coEvery { catalog.checkAvailability(2, 1) } returns Availability(true, 9, true)
        vm.checkout()
        assertEquals(CartEvent.Error("Sin stock suficiente: Producto 1"), vm.event.value)
        coVerify(exactly = 0) { orders.placeOrder(any(), any(), any(), any()) }
    }

    @Test fun `checkout exitoso crea el pedido`() = runTest {
        val vm = vm()
        keepCollecting(vm.state)
        coEvery { catalog.checkAvailability(any(), any()) } returns Availability(true, 9, true)
        val order = Fixtures.order(id = 9)
        coEvery { orders.placeOrder(any(), "Chile", "EXPRESS", any()) } returns order

        vm.setType(ShippingType.EXPRESS)
        vm.setQty(1, 3)
        vm.checkout()

        assertEquals(CartEvent.Placed(order), vm.event.value)
        assertFalse(vm.busy.value)
        coVerify { catalog.setQuantity(1, 3) }
        vm.consumeEvent()
        assertNull(vm.event.value)
    }

    @Test fun `checkout sin servidor muestra error y no crea el pedido`() = runTest {
        val vm = vm()
        keepCollecting(vm.state)
        coEvery { catalog.checkAvailability(any(), any()) } returns Availability(true, 9, false)
        coEvery { orders.placeOrder(any(), any(), any(), any()) } throws IllegalStateException("No se pudo conectar con el servidor")

        vm.checkout()

        assertEquals(CartEvent.Error("No se pudo conectar con el servidor"), vm.event.value)
        assertFalse(vm.busy.value)
    }

    @Test fun `checkout con carrito vacio no hace nada`() = runTest {
        lines.value = emptyList()
        val vm = vm()
        keepCollecting(vm.state)

        vm.checkout()

        assertNull(vm.event.value)
        coVerify(exactly = 0) { catalog.checkAvailability(any(), any()) }
    }
}
