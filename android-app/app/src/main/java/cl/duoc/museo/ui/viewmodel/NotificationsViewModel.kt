package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.NotificationItem
import cl.duoc.museo.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsState(val items: List<NotificationItem> = emptyList(), val filter: String? = null)

class NotificationsViewModel(private val repo: NotificationRepository) : ViewModel() {
    private val filter = MutableStateFlow<String?>(null)

    val state: StateFlow<NotificationsState> = combine(repo.observeMine(), filter) { list, f ->
        NotificationsState(if (f == null) list else list.filter { it.notification.category == f }, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsState())

    private val _sent = MutableStateFlow(false)
    val sent: StateFlow<Boolean> = _sent.asStateFlow()

    init { viewModelScope.launch { repo.refresh() } }

    fun setFilter(f: String?) { filter.value = f }
    fun markRead(id: Long) = viewModelScope.launch { repo.markRead(id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }

    /** Aviso de prueba originado por el Administrador. */
    fun sendTest(title: String, body: String, category: String, targetRole: String) = viewModelScope.launch {
        val destination = when (category) {
            "CURSO", "PUBLICACION", "EVENTO" -> "catalog"
            "MUSEO" -> "map"
            "PEDIDO" -> "orders"
            else -> "notifications"
        }
        repo.publish(title, body, category, targetRole = targetRole, destination = destination, sendToBackend = true)
        _sent.value = true
    }

    fun consumeSent() { _sent.value = false }
}
