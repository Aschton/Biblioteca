package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
