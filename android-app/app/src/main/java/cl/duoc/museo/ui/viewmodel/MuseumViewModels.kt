package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.QrHistoryEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.data.repository.OrderStatus
import cl.duoc.museo.data.repository.QrRepository
import cl.duoc.museo.util.Validators
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

// ---------------- Dashboard ----------------

data class DashboardState(
    val featuredMuseums: List<MuseumEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val pendingOrders: List<OrderEntity> = emptyList(),
    val unpublishedMuseums: List<MuseumEntity> = emptyList(),
    val totalMuseums: Int = 0,
    val online: Boolean? = null
)

class DashboardViewModel(
    private val museums: MuseumRepository,
    private val catalog: CatalogRepository,
    private val orders: OrderRepository
) : ViewModel() {

    private val online = MutableStateFlow<Boolean?>(null)

    val state: StateFlow<DashboardState> = combine(
        museums.observeMuseums(includeUnpublished = true),
        catalog.observeProducts(includeInactive = false),
        orders.observeAll(),
        online
    ) { allMuseums, products, allOrders, isOnline ->
        DashboardState(
            featuredMuseums = allMuseums.filter { it.published }.sortedByDescending { it.rating }.take(6),
            products = products.take(6),
            pendingOrders = allOrders.filter { it.status == OrderStatus.PENDIENTE },
            unpublishedMuseums = allMuseums.filter { !it.published || it.pendingSync },
            totalMuseums = allMuseums.size,
            online = isOnline
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val ok = museums.refresh()
        catalog.refresh()
        orders.refresh()
        online.value = ok
    }
}

// ---------------- Mapa ----------------

enum class MapFilter { TODOS, PAIS, PROYECTO }

data class MapState(
    val museums: List<MuseumEntity> = emptyList(),
    val filter: MapFilter = MapFilter.TODOS,
    val filterValue: String? = null,
    val options: List<String> = emptyList(),
    val selected: MuseumEntity? = null,
    val online: Boolean? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(private val repo: MuseumRepository, session: SessionManager) : ViewModel() {

    private val filter = MutableStateFlow(MapFilter.TODOS)
    private val filterValue = MutableStateFlow<String?>(null)
    private val selected = MutableStateFlow<MuseumEntity?>(null)
    private val online = MutableStateFlow<Boolean?>(null)

    // Admin e Investigador también ven fichas pendientes de publicación
    private val source = session.currentUser.flatMapLatest { u ->
        repo.observeMuseums(includeUnpublished = u != null && u.role != Role.CLIENTE.name)
    }

    val state: StateFlow<MapState> = combine(source, filter, filterValue, selected, online) { all, f, v, sel, on ->
        val options = when (f) {
            MapFilter.TODOS -> emptyList()
            MapFilter.PAIS -> all.map { it.country }.distinct().sorted()
            MapFilter.PROYECTO -> all.map { it.project }.distinct().sorted()
        }
        val list = when {
            f == MapFilter.PAIS && v != null -> all.filter { it.country == v }
            f == MapFilter.PROYECTO && v != null -> all.filter { it.project == v }
            else -> all
        }
        MapState(list, f, v, options, sel, on)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState())

    init { viewModelScope.launch { online.value = repo.refresh() } }

    fun setFilter(f: MapFilter) { filter.value = f; filterValue.value = null }
    fun setFilterValue(v: String?) { filterValue.value = v; selected.value = null }
    fun select(m: MuseumEntity?) { selected.value = m }
}

// ---------------- Detalle de museo ----------------

class MuseumDetailViewModel(handle: SavedStateHandle, private val repo: MuseumRepository) : ViewModel() {
    private val id: Long = checkNotNull(handle.get<Long>("id"))

    val museum: StateFlow<MuseumEntity?> = repo.observeMuseum(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isFavorite: StateFlow<Boolean> = repo.observeIsFavorite(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { viewModelScope.launch { repo.recordView(id) } }

    fun toggleFavorite() = viewModelScope.launch {
        val fav = !isFavorite.value
        repo.setFavorite(id, fav)
        _message.value = if (fav) "Guardado en favoritos" else "Quitado de favoritos"
    }

    fun setPublished(published: Boolean) = viewModelScope.launch {
        val m = museum.value ?: return@launch
        val online = repo.setPublished(m, published)
        _message.value = (if (published) "Ficha publicada" else "Publicación retirada") +
            if (online) "" else " (guardado localmente)"
    }

    fun clearMessage() { _message.value = null }
}

// ---------------- Formulario de ficha museológica (Investigador) ----------------

data class MuseumForm(
    val name: String = "",
    val country: String = "Chile",
    val city: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val description: String = "",
    val classification: String = "Historia",
    val project: String = "Red Iberoamericana",
    val virtualTourUrl: String = "https://",
    val imageUrl: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val savedOnline: Boolean = false
)

class MuseumFormViewModel(
    handle: SavedStateHandle,
    private val repo: MuseumRepository
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    val isNew = id <= 0
    private var original: MuseumEntity? = null

    private val _form = MutableStateFlow(MuseumForm())
    val form: StateFlow<MuseumForm> = _form.asStateFlow()

    init {
        if (!isNew) viewModelScope.launch {
            repo.getMuseum(id)?.let { m ->
                original = m
                _form.value = MuseumForm(
                    m.name, m.country, m.city, m.latitude.toString(), m.longitude.toString(),
                    m.description, m.classification, m.project, m.virtualTourUrl, m.imageUrl
                )
            }
        }
    }

    fun update(t: (MuseumForm) -> MuseumForm) = _form.update(t)

    fun save(publishDirectly: Boolean) {
        val f = _form.value
        val lat = f.latitude.replace(',', '.').toDoubleOrNull()
        val lng = f.longitude.replace(',', '.').toDoubleOrNull()
        val errors = buildMap {
            if (f.name.isBlank()) put("name", "Obligatorio")
            if (f.city.isBlank()) put("city", "Obligatorio")
            if (lat == null || lat !in -60.0..50.0) put("latitude", "Latitud inválida")
            if (lng == null || lng !in -120.0..10.0) put("longitude", "Longitud inválida (Iberoamérica)")
            if (f.description.length < 20) put("description", "Mínimo 20 caracteres")
            if (!Validators.isUrl(f.virtualTourUrl) || f.virtualTourUrl.length < 12) put("virtualTourUrl", "Enlace no válido")
        }
        _form.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            _form.update { it.copy(saving = true) }
            val entity = (original ?: MuseumEntity(
                id = 0, name = "", country = "", city = "", latitude = 0.0, longitude = 0.0, description = "",
                classification = "", project = "", virtualTourUrl = "", qrCode = ""
            )).copy(
                name = f.name.trim(), country = f.country, city = f.city.trim(), latitude = lat!!, longitude = lng!!,
                description = f.description.trim(), classification = f.classification, project = f.project,
                virtualTourUrl = f.virtualTourUrl.trim(), imageUrl = f.imageUrl,
                published = if (isNew) publishDirectly else (original?.published ?: false)
            )
            val online = repo.save(entity, isNew)
            // TODO (pendiente): notificaciones push: avisar al Administrador que hay una ficha por revisar.
            _form.update { it.copy(saving = false, saved = true, savedOnline = online) }
        }
    }
}

// ---------------- Favoritos ----------------

data class FavoritesState(
    val museums: List<MuseumEntity> = emptyList(),
    val viewed: List<MuseumEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val qrHistory: List<QrHistoryEntity> = emptyList()
)

class FavoritesViewModel(
    private val museumsRepo: MuseumRepository,
    private val catalog: CatalogRepository,
    qr: QrRepository
) : ViewModel() {
    val state: StateFlow<FavoritesState> = combine(
        museumsRepo.observeFavorites(),
        museumsRepo.observeViewed(),
        catalog.observeFavoriteProducts(),
        qr.observeHistory()
    ) { m, v, p, q -> FavoritesState(m, v, p, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesState())

    fun removeMuseum(id: Long) = viewModelScope.launch { museumsRepo.setFavorite(id, false) }
    fun removeProduct(id: Long) = viewModelScope.launch { catalog.setFavorite(id, false) }
}
