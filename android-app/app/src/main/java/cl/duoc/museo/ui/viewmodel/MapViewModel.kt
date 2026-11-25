package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.MuseumRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
