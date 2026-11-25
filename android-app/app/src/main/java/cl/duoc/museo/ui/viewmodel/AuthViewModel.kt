package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

data class RegisterForm(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val avatar: String = "avatar_1",
    val interests: Set<String> = emptySet(),
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null
)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _form = MutableStateFlow(RegisterForm())
    val form: StateFlow<RegisterForm> = _form.asStateFlow()

    fun login(email: String, password: String, role: Role) {
        if (!Validators.isEmail(email)) { _state.update { it.copy(error = "Ingresa un correo válido") }; return }
        if (password.isBlank()) { _state.update { it.copy(error = "Ingresa tu contraseña") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.login(email, password, role)
                .onSuccess { _state.value = AuthUiState(loggedIn = true) }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun updateForm(transform: (RegisterForm) -> RegisterForm) = _form.update(transform)

    fun register() {
        val f = _form.value
        val checked = f.copy(
            nameError = if (f.name.trim().length < 3) "Ingresa tu nombre completo" else null,
            emailError = if (!Validators.isEmail(f.email)) "Correo no válido" else null,
            passwordError = if (!Validators.isStrongPassword(f.password)) "Mínimo 6 caracteres, con letras y números" else null,
            confirmError = if (f.confirm != f.password) "Las contraseñas no coinciden" else null
        )
        _form.value = checked
        if (listOf(checked.nameError, checked.emailError, checked.passwordError, checked.confirmError).any { it != null }) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.register(f.name, f.email, f.password, f.avatar, f.interests.joinToString(","))
                .onSuccess { _state.value = AuthUiState(loggedIn = true) }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
