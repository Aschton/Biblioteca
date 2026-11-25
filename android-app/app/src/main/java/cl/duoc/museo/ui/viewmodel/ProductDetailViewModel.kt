package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProductEvent {
    data class Added(val qty: Int) : ProductEvent
    data class Error(val message: String) : ProductEvent
    data object InfoRequested : ProductEvent
}

class ProductDetailViewModel(
    handle: SavedStateHandle,
    private val repo: CatalogRepository,
    private val orders: OrderRepository
) : ViewModel() {
    private val id: Long = checkNotNull(handle.get<Long>("id"))

    val product: StateFlow<ProductEntity?> = repo.observeProduct(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val isFavorite: StateFlow<Boolean> = repo.observeIsFavorite(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _qty = MutableStateFlow(1)
    val qty: StateFlow<Int> = _qty.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _event = MutableStateFlow<ProductEvent?>(null)
    val event: StateFlow<ProductEvent?> = _event.asStateFlow()

    fun inc() { val max = product.value?.stock ?: 1; _qty.update { (it + 1).coerceAtMost(max.coerceAtLeast(1)) } }
    fun dec() { _qty.update { (it - 1).coerceAtLeast(1) } }

    fun toggleFavorite() = viewModelScope.launch { repo.setFavorite(id, !isFavorite.value) }

    /** Valida disponibilidad con el backend antes de agregar al carrito. */
    fun addToCart() = viewModelScope.launch {
        _busy.value = true
        val a = repo.checkAvailability(id, _qty.value)
        _event.value = if (a.available) {
            repo.addToCart(id, _qty.value); ProductEvent.Added(_qty.value)
        } else ProductEvent.Error("Sin disponibilidad suficiente (stock: ${a.stock})")
        _busy.value = false
    }

    fun requestInfo() = viewModelScope.launch {
        val p = product.value ?: return@launch
        _busy.value = true
        _event.value = try {
            orders.requestCourseInfo(p)
            ProductEvent.InfoRequested
        } catch (e: Exception) {
            ProductEvent.Error(e.message ?: "No se pudo conectar con el servidor")
        }
        _busy.value = false
    }

    fun consumeEvent() { _event.value = null }
}
