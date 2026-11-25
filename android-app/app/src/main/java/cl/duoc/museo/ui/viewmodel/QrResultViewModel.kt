package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.repository.QrRepository
import cl.duoc.museo.data.repository.QrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QrState(val loading: Boolean = true, val result: QrResult? = null, val error: String? = null)

class QrResultViewModel(handle: SavedStateHandle, private val repo: QrRepository) : ViewModel() {
    private val code: String = handle.get<String>("code").orEmpty()
    private val _state = MutableStateFlow(QrState())
    val state: StateFlow<QrState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.resolve(code)
                .onSuccess { _state.value = QrState(false, it) }
                .onFailure { _state.value = QrState(false, null, it.message) }
        }
    }
}
