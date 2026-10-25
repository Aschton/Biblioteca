package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.CartLine
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.isCourse
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.data.repository.QrRepository
import cl.duoc.museo.data.repository.QrResult
import cl.duoc.museo.util.ShippingCalculator
import cl.duoc.museo.util.ShippingCalculator.ShippingType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- Resultado QR ----------------

data class QrState(val loading: Boolean = true, val result: QrResult? = null, val error: String? = null)

class QrResultViewModel(handle: SavedStateHandle, private val repo: QrRepository) : ViewModel() {
    private val code: String = handle.get<String>("code").orEmpty()
    private val _state = MutableStateFlow(QrState())
    val state: StateFlow<QrState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.resolve(code)
                .onSuccess { _state.value = QrState(false, it) }
                .onFailure { _state.value = QrState(false, null, it.message) }
        }
    }
}

// ---------------- Catálogo ----------------

val categories = listOf("Todos" to null, "Cursos" to "CURSO", "Libros" to "LIBRO", "Materiales" to "MATERIAL", "Artículos" to "ARTICULO")

data class CatalogState(
    val products: List<ProductEntity> = emptyList(),
    val query: String = "",
    val category: String? = null,
    val onlyAvailable: Boolean = false,
    val refreshing: Boolean = false,
    val online: Boolean? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(private val repo: CatalogRepository, session: SessionManager) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)
    private val onlyAvailable = MutableStateFlow(false)
    private val status = MutableStateFlow<Pair<Boolean, Boolean?>>(false to null)

    private val source = session.currentUser.flatMapLatest { u ->
        repo.observeProducts(includeInactive = u?.role == Role.ADMIN.name)
    }

    val state: StateFlow<CatalogState> = combine(source, query, category, onlyAvailable, status) { all, q, c, avail, st ->
        val filtered = all.filter { p ->
            (c == null || p.category == c) &&
                (q.isBlank() || p.name.contains(q, true) || p.description.contains(q, true)) &&
                (!avail || p.stock > 0)
        }
        CatalogState(filtered, q, c, avail, st.first, st.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogState())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        status.value = true to status.value.second
        val ok = repo.refresh()
        status.value = false to ok
    }

    fun setQuery(q: String) { query.value = q }
    fun setCategory(c: String?) { category.value = c }
    fun toggleAvailable() { onlyAvailable.update { !it } }
}

// ---------------- Detalle de producto y solicitud ----------------

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
        orders.requestCourseInfo(p)
        _event.value = ProductEvent.InfoRequested
        _busy.value = false
    }

    fun consumeEvent() { _event.value = null }
}

// ---------------- Formulario de producto (Administrador) ----------------

data class ProductForm(
    val name: String = "",
    val category: String = "CURSO",
    val description: String = "",
    val price: String = "",
    val stock: String = "",
    val imageUrl: String = "",
    val active: Boolean = true,
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val savedOnline: Boolean = false
)

class ProductFormViewModel(
    handle: SavedStateHandle,
    private val repo: CatalogRepository
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    val isNew = id <= 0
    private var original: ProductEntity? = null

    private val _form = MutableStateFlow(ProductForm())
    val form: StateFlow<ProductForm> = _form.asStateFlow()

    init {
        if (!isNew) viewModelScope.launch {
            repo.getProduct(id)?.let { p ->
                original = p
                _form.value = ProductForm(p.name, p.category, p.description, p.price.toString(), p.stock.toString(), p.imageUrl, p.active)
            }
        }
    }

    fun update(t: (ProductForm) -> ProductForm) = _form.update(t)

    fun save() {
        val f = _form.value
        val price = f.price.replace(',', '.').toDoubleOrNull()
        val stock = f.stock.toIntOrNull()
        val errors = buildMap {
            if (f.name.trim().length < 3) put("name", "Mínimo 3 caracteres")
            if (f.description.trim().length < 10) put("description", "Mínimo 10 caracteres")
            if (price == null || price <= 0) put("price", "Precio mayor a 0")
            if (stock == null || stock < 0) put("stock", "Número entero ≥ 0")
        }
        _form.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            _form.update { it.copy(saving = true) }
            val entity = (original ?: ProductEntity(0, "", "", "", 0.0, "", 0, true, "")).copy(
                name = f.name.trim(), category = f.category, description = f.description.trim(),
                price = price!!, stock = stock!!, imageUrl = f.imageUrl, active = f.active
            )
            val online = repo.save(entity, isNew)
            // TODO (pendiente): notificaciones push: avisar a los clientes del nuevo curso o producto.
            _form.update { it.copy(saving = false, saved = true, savedOnline = online) }
        }
    }

    fun deactivate() = viewModelScope.launch {
        val p = original ?: return@launch
        val online = repo.setActive(p, false)
        _form.update { it.copy(active = false, saved = true, savedOnline = online) }
    }
}

// ---------------- Carrito y envío ----------------

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
            val order = orders.placeOrder(s.lines, s.country, s.type.name, s.shipping)
            _event.value = CartEvent.Placed(order)
        }
        _busy.value = false
    }

    fun consumeEvent() { _event.value = null }
}
