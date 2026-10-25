package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.util.Validators
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

// ---------------- Historial y gestión de solicitudes ----------------

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

    init { refresh() }

    fun refresh() = viewModelScope.launch { online.value = repo.refresh() }
    fun setFilter(f: String?) { filter.value = f }
    fun updateStatus(order: OrderEntity, status: String) = viewModelScope.launch { repo.updateStatus(order, status) }
}

// ---------------- Notificaciones ----------------
// TODO (pendiente): NotificationsViewModel (listar avisos, marcar como leídos, enviar avisos de prueba).

// ---------------- Perfil y configuración ----------------

class ProfileViewModel(private val auth: AuthRepository, session: SessionManager) : ViewModel() {
    val user: StateFlow<UserEntity?> = session.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun save(transform: (UserEntity) -> UserEntity, confirmation: String = "Cambios guardados") = viewModelScope.launch {
        val u = user.value ?: return@launch
        auth.update(transform(u))
        _message.value = confirmation
    }

    fun saveData(name: String, email: String, address: String): Boolean {
        if (name.trim().length < 3) { _message.value = "El nombre es muy corto"; return false }
        if (!Validators.isEmail(email)) { _message.value = "Correo no válido"; return false }
        save({ it.copy(name = name.trim(), email = email.trim().lowercase(), shippingAddress = address.trim()) })
        return true
    }

    fun changePassword(current: String, new: String) = viewModelScope.launch {
        val u = user.value ?: return@launch
        if (!Validators.isStrongPassword(new)) { _message.value = "La nueva contraseña debe tener letras y números (mín. 6)"; return@launch }
        _message.value = auth.changePassword(u, current, new).fold({ "Contraseña actualizada" }, { it.message })
    }

    fun logout() = auth.logout()
    fun clearMessage() { _message.value = null }
}

// ---------------- Cuentas de demostración (Administrador) ----------------

class AdminUsersViewModel(private val auth: AuthRepository) : ViewModel() {
    val users: StateFlow<List<UserEntity>> = auth.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun create(name: String, email: String, password: String, role: Role) = viewModelScope.launch {
        _message.value = when {
            name.isBlank() -> "Ingresa un nombre"
            !Validators.isEmail(email) -> "Correo no válido"
            !Validators.isStrongPassword(password) -> "Contraseña: mín. 6, letras y números"
            else -> auth.createUser(name, email, password, role).fold({ "Cuenta creada" }, { it.message })
        }
    }

    fun toggle(user: UserEntity) = viewModelScope.launch { auth.setActive(user, !user.active) }
    fun delete(user: UserEntity) = viewModelScope.launch { auth.deleteUser(user) }
    fun clearMessage() { _message.value = null }
}
