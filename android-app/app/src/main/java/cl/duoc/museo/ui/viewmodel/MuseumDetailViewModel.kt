package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.WeatherInfo
import cl.duoc.museo.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la tarjeta de clima (API externa Open-Meteo). */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val info: WeatherInfo) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class MuseumDetailViewModel(
    handle: SavedStateHandle,
    private val repo: MuseumRepository,
    private val weatherRepo: WeatherRepository
) : ViewModel() {
    private val id: Long = checkNotNull(handle.get<Long>("id"))

    val museum: StateFlow<MuseumEntity?> = repo.observeMuseum(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isFavorite: StateFlow<Boolean> = repo.observeIsFavorite(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        viewModelScope.launch { repo.recordView(id) }
        // Espera a tener la ficha (con sus coordenadas) y consulta el clima una vez
        viewModelScope.launch {
            val m = repo.observeMuseum(id).filterNotNull().first()
            loadWeather(m.latitude, m.longitude)
        }
    }

    private suspend fun loadWeather(lat: Double, lng: Double) {
        _weather.value = WeatherUiState.Loading
        _weather.value = weatherRepo.getWeather(lat, lng).fold(
            onSuccess = { WeatherUiState.Success(it) },
            onFailure = { WeatherUiState.Error("No se pudo obtener el clima. Revisa tu conexión a internet.") }
        )
    }

    fun retryWeather() = viewModelScope.launch {
        val m = museum.value ?: repo.getMuseum(id) ?: return@launch
        loadWeather(m.latitude, m.longitude)
    }

    /** Solo el Administrador. Requiere que ms-museos esté disponible. */
    fun delete() = viewModelScope.launch {
        val m = museum.value ?: repo.getMuseum(id) ?: return@launch
        if (repo.delete(m)) _deleted.value = true
        else _message.value = "No se pudo eliminar: el servidor no responde"
    }

    fun toggleFavorite() = viewModelScope.launch {
        val fav = !isFavorite.value
        repo.setFavorite(id, fav)
        _message.value = if (fav) "Guardado en favoritos" else "Quitado de favoritos"
    }

    fun setPublished(published: Boolean) = viewModelScope.launch {
        val m = museum.value ?: return@launch
        val online = repo.setPublished(m, published)
        _message.value = (if (published) "Ficha publicada" else "Publicación retirada") +
            if (online) "" else " (guardado localmente)"
    }

    fun clearMessage() { _message.value = null }
}
