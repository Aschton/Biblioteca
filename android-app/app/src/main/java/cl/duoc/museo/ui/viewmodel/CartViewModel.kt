package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.CartLine
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.isCourse
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.util.ShippingCalculator
import cl.duoc.museo.util.ShippingCalculator.ShippingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartState(
    val lines: List<CartLine> = emptyList(),
    val country: String = "Chile",
    val type: ShippingType = ShippingType.ESTANDAR,
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0,
    val shippingError: String? = null,
    val onlyDigital: Boolean = false
)

sealed interface CartEvent {
    data class Placed(val order: OrderEntity) : CartEvent
    data class Error(val message: String) : CartEvent
}

class CartViewModel(private val catalog: CatalogRepository, private val orders: OrderRepository) : ViewModel() {
    private val country = MutableStateFlow("Chile")
    private val type = MutableStateFlow(ShippingType.ESTANDAR)

    val state: StateFlow<CartState> = combine(catalog.observeCart(), country, type) { lines, c, t ->
        val subtotal = lines.sumOf { it.product.price * it.quantity }
        val physical = lines.filter { !it.product.isCourse }.sumOf { it.quantity }
        val quote = ShippingCalculator.quote(c, t, physical)
        val error = if (quote < 0) "El retiro en sede solo está disponible en Chile" else null
        val ship = quote.coerceAtLeast(0.0)
        CartState(lines, c, t, subtotal, ship, subtotal + ship, error, lines.isNotEmpty() && physical == 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartState())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _event = MutableStateFlow<CartEvent?>(null)
    val event: StateFlow<CartEvent?> = _event.asStateFlow()

    fun setCountry(c: String) { country.value = c }
    fun setType(t: ShippingType) { type.value = t }
    fun setQty(productId: Long, qty: Int) = viewModelScope.launch { catalog.setQuantity(productId, qty) }

    fun checkout() = viewModelScope.launch {
        val s = state.value
        if (s.lines.isEmpty()) return@launch
        if (s.shippingError != null) { _event.value = CartEvent.Error(s.shippingError); return@launch }
        _busy.value = true
        val missing = s.lines.filter { !catalog.checkAvailability(it.product.id, it.quantity).available }
        if (missing.isNotEmpty()) {
            _event.value = CartEvent.Error("Sin stock suficiente: " + missing.joinToString { it.product.name })
        } else {
            _event.value = try {
                CartEvent.Placed(orders.placeOrder(s.lines, s.country, s.type.name, s.shipping))
            } catch (e: Exception) {
                // Servidor o base de datos caídos: no se compra y el carrito queda intacto
                CartEvent.Error(e.message ?: "No se pudo conectar con el servidor")
            }
        }
        _busy.value = false
    }

    fun consumeEvent() { _event.value = null }
}
