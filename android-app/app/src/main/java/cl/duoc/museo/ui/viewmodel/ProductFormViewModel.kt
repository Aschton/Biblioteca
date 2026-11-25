package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductForm(
    val name: String = "",
    val category: String = "CURSO",
    val description: String = "",
    val price: String = "",
    val stock: String = "",
    val imageUrl: String = "",
    val active: Boolean = true,
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val savedOnline: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class ProductFormViewModel(
    handle: SavedStateHandle,
    private val repo: CatalogRepository,
    private val notifications: NotificationRepository
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    val isNew = id <= 0
    private var original: ProductEntity? = null

    private val _form = MutableStateFlow(ProductForm())
    val form: StateFlow<ProductForm> = _form.asStateFlow()

    init {
        if (!isNew) viewModelScope.launch {
            repo.getProduct(id)?.let { p ->
                original = p
                _form.value = ProductForm(p.name, p.category, p.description, p.price.toString(), p.stock.toString(), p.imageUrl, p.active)
            }
        }
    }

    fun update(t: (ProductForm) -> ProductForm) = _form.update(t)

    fun save() {
        val f = _form.value
        val price = f.price.replace(',', '.').toDoubleOrNull()
        val stock = f.stock.toIntOrNull()
        val errors = buildMap {
            if (f.name.trim().length < 3) put("name", "Mínimo 3 caracteres")
            if (f.description.trim().length < 10) put("description", "Mínimo 10 caracteres")
            if (price == null || price <= 0) put("price", "Precio mayor a 0")
            if (stock == null || stock < 0) put("stock", "Número entero ≥ 0")
        }
        _form.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            _form.update { it.copy(saving = true) }
            val entity = (original ?: ProductEntity(0, "", "", "", 0.0, "", 0, true, "")).copy(
                name = f.name.trim(), category = f.category, description = f.description.trim(),
                price = price!!, stock = stock!!, imageUrl = f.imageUrl, active = f.active
            )
            val online = repo.save(entity, isNew)
            if (isNew && entity.active) {
                val kind = if (entity.category == "CURSO") "CURSO" else "PUBLICACION"
                notifications.publish(
                    if (kind == "CURSO") "Nuevo curso disponible" else "Nuevo en el catálogo",
                    entity.name, kind, targetRole = Role.CLIENTE.name, destination = "catalog"
                )
            }
            _form.update { it.copy(saving = false, saved = true, savedOnline = online) }
        }
    }

    fun deactivate() = viewModelScope.launch {
        val p = original ?: return@launch
        val online = repo.setActive(p, false)
        _form.update { it.copy(active = false, saved = true, savedOnline = online) }
    }

    /** Elimina definitivamente el producto (DELETE en ms-catalogo). */
    fun delete() = viewModelScope.launch {
        val p = original ?: return@launch
        _form.update { it.copy(saving = true, error = null) }
        val ok = repo.delete(p)
        _form.update {
            if (ok) it.copy(saving = false, deleted = true)
            else it.copy(saving = false, error = "No se pudo eliminar: el servidor no responde")
        }
    }

    fun clearError() = _form.update { it.copy(error = null) }
}
