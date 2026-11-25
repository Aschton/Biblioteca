package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MuseumForm(
    val name: String = "",
    val country: String = "Chile",
    val city: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val description: String = "",
    val classification: String = "Historia",
    val project: String = "Red Iberoamericana",
    val virtualTourUrl: String = "https://",
    val imageUrl: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val savedOnline: Boolean = false
)

class MuseumFormViewModel(
    handle: SavedStateHandle,
    private val repo: MuseumRepository,
    private val notifications: NotificationRepository
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    val isNew = id <= 0
    private var original: MuseumEntity? = null

    private val _form = MutableStateFlow(MuseumForm())
    val form: StateFlow<MuseumForm> = _form.asStateFlow()

    init {
        if (!isNew) viewModelScope.launch {
            repo.getMuseum(id)?.let { m ->
                original = m
                _form.value = MuseumForm(
                    m.name, m.country, m.city, m.latitude.toString(), m.longitude.toString(),
                    m.description, m.classification, m.project, m.virtualTourUrl, m.imageUrl
                )
            }
        }
    }

    fun update(t: (MuseumForm) -> MuseumForm) = _form.update(t)

    fun save(publishDirectly: Boolean) {
        val f = _form.value
        val lat = f.latitude.replace(',', '.').toDoubleOrNull()
        val lng = f.longitude.replace(',', '.').toDoubleOrNull()
        val errors = buildMap {
            if (f.name.isBlank()) put("name", "Obligatorio")
            if (f.city.isBlank()) put("city", "Obligatorio")
            if (lat == null || lat !in -60.0..50.0) put("latitude", "Latitud inválida")
            if (lng == null || lng !in -120.0..10.0) put("longitude", "Longitud inválida (Iberoamérica)")
            if (f.description.length < 20) put("description", "Mínimo 20 caracteres")
            if (!Validators.isUrl(f.virtualTourUrl) || f.virtualTourUrl.length < 12) put("virtualTourUrl", "Enlace no válido")
        }
        _form.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            _form.update { it.copy(saving = true) }
            val entity = (original ?: MuseumEntity(
                id = 0, name = "", country = "", city = "", latitude = 0.0, longitude = 0.0, description = "",
                classification = "", project = "", virtualTourUrl = "", qrCode = ""
            )).copy(
                name = f.name.trim(), country = f.country, city = f.city.trim(), latitude = lat!!, longitude = lng!!,
                description = f.description.trim(), classification = f.classification, project = f.project,
                virtualTourUrl = f.virtualTourUrl.trim(), imageUrl = f.imageUrl,
                published = if (isNew) publishDirectly else (original?.published ?: false)
            )
            val online = repo.save(entity, isNew)
            notifications.publish(
                if (isNew) "Nueva ficha registrada" else "Ficha actualizada",
                "${entity.name} (${entity.country}) espera revisión",
                "MUSEO", targetRole = Role.ADMIN.name, destination = "map"
            )
            _form.update { it.copy(saving = false, saved = true, savedOnline = online) }
        }
    }
}
