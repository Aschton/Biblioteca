package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersState(
    val orders: List<OrderEntity> = emptyList(),
    val filter: String? = null,
    val isAdmin: Boolean = false,
    val online: Boolean? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModel(private val repo: OrderRepository, session: SessionManager) : ViewModel() {
    private val filter = MutableStateFlow<String?>(null)
    private val online = MutableStateFlow<Boolean?>(null)

    private val source = session.currentUser.flatMapLatest { u ->
        val admin = u?.role == Role.ADMIN.name
        (if (admin) repo.observeAll() else repo.observeMine()).map { it to admin }
    }

    val state: StateFlow<OrdersState> = combine(source, filter, online) { (list, admin), f, on ->
        OrdersState(if (f == null) list else list.filter { it.status == f }, f, admin, on)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch { online.value = repo.refresh() }
    fun setFilter(f: String?) { filter.value = f }
    fun updateStatus(order: OrderEntity, status: String) = viewModelScope.launch { repo.updateStatus(order, status) }

    /** El Administrador elimina una solicitud (DELETE en ms-pedidos). */
    fun delete(order: OrderEntity) = viewModelScope.launch {
        _message.value = if (repo.delete(order)) "Solicitud ${order.code} eliminada"
        else "No se pudo eliminar: el servidor no responde"
    }

    fun clearMessage() { _message.value = null }
}
