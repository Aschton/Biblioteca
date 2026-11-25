package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
