package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class UserState(val loading: Boolean = true, val user: UserEntity? = null)

/** Estado global: usuario activo, contadores de la barra y cierre de sesión. */
class SessionViewModel(
    private val session: SessionManager,
    private val auth: AuthRepository,
    notifications: NotificationRepository,
    catalog: CatalogRepository
) : ViewModel() {

    val userState: StateFlow<UserState> = session.currentUser
        .map { UserState(loading = false, user = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserState())

    val unread: StateFlow<Int> = notifications.observeUnread()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val cartCount: StateFlow<Int> = catalog.observeCartCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun hasSession() = session.currentId() != null

    fun logout() = auth.logout()
}
