package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
