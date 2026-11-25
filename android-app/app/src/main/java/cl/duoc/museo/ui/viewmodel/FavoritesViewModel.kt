package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.QrHistoryEntity
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.QrRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesState(
    val museums: List<MuseumEntity> = emptyList(),
    val viewed: List<MuseumEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val qrHistory: List<QrHistoryEntity> = emptyList()
)

class FavoritesViewModel(
    private val museumsRepo: MuseumRepository,
    private val catalog: CatalogRepository,
    qr: QrRepository
) : ViewModel() {
    val state: StateFlow<FavoritesState> = combine(
        museumsRepo.observeFavorites(),
        museumsRepo.observeViewed(),
        catalog.observeFavoriteProducts(),
        qr.observeHistory()
    ) { m, v, p, q -> FavoritesState(m, v, p, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesState())

    fun removeMuseum(id: Long) = viewModelScope.launch { museumsRepo.setFavorite(id, false) }
    fun removeProduct(id: Long) = viewModelScope.launch { catalog.setFavorite(id, false) }
}
