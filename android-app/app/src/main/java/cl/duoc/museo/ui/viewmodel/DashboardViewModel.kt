package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.data.repository.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val featuredMuseums: List<MuseumEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val pendingOrders: List<OrderEntity> = emptyList(),
    val unpublishedMuseums: List<MuseumEntity> = emptyList(),
    val totalMuseums: Int = 0,
    val online: Boolean? = null
)

class DashboardViewModel(
    private val museums: MuseumRepository,
    private val catalog: CatalogRepository,
    private val orders: OrderRepository,
    private val notifications: NotificationRepository
) : ViewModel() {

    private val online = MutableStateFlow<Boolean?>(null)

    val state: StateFlow<DashboardState> = combine(
        museums.observeMuseums(includeUnpublished = true),
        catalog.observeProducts(includeInactive = false),
        orders.observeAll(),
        online
    ) { allMuseums, products, allOrders, isOnline ->
        DashboardState(
            featuredMuseums = allMuseums.filter { it.published }.sortedByDescending { it.rating }.take(6),
            products = products.take(6),
            pendingOrders = allOrders.filter { it.status == OrderStatus.PENDIENTE },
            unpublishedMuseums = allMuseums.filter { !it.published || it.pendingSync },
            totalMuseums = allMuseums.size,
            online = isOnline
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val ok = museums.refresh()
        catalog.refresh()
        orders.refresh()
        notifications.refresh()
        online.value = ok
    }
}
