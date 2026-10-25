package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.CartLine
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.remote.ApiService
import cl.duoc.museo.data.remote.OrderDto
import cl.duoc.museo.data.remote.OrderItemRequest
import cl.duoc.museo.data.remote.OrderRequest
import cl.duoc.museo.data.remote.StatusUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

object OrderStatus {
    const val PENDIENTE = "PENDIENTE"
    const val CONFIRMADO = "CONFIRMADO"
    const val ENVIADO = "ENVIADO"
    const val ENTREGADO = "ENTREGADO"
    val all = listOf(PENDIENTE, CONFIRMADO, ENVIADO, ENTREGADO)
    fun label(s: String) = s.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Pedidos y solicitudes. El backend centraliza el pedido y su estado; SQLite guarda
 * un historial local de consulta (y el pedido si no hubo conexión, marcado como no sincronizado).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    private val session: SessionManager,
    private val catalog: CatalogRepository
) {
    private val dao = db.orderDao()

    fun observeMine(): Flow<List<OrderEntity>> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.observeByUser(id)
    }

    fun observeAll(): Flow<List<OrderEntity>> = dao.observeAll()

    suspend fun refresh(): Boolean = runCatching {
        val user = session.current() ?: return@runCatching
        dao.unsynced().forEach { local -> runCatching { resend(local) } }
        val remote = if (user.role == Role.ADMIN.name) api.orders() else api.orders(user.email)
        dao.upsertAll(remote.map { it.toEntity() })
    }.isSuccess

    private suspend fun resend(local: OrderEntity) {
        // El pedido offline no guarda las líneas detalladas; se reenvía como solicitud resumida.
        val dto = api.createOrder(
            OrderRequest(local.userEmail, local.userName, emptyList(), local.country, local.shippingType, local.kind)
        )
        dao.deleteById(local.id)
        dao.upsert(dto.toEntity().copy(itemsSummary = local.itemsSummary, itemsCount = local.itemsCount,
            subtotal = local.subtotal, shipping = local.shipping, total = local.total))
    }

    /** Confirma la compra del carrito. Devuelve el pedido guardado. */
    suspend fun placeOrder(lines: List<CartLine>, country: String, shippingType: String, shipping: Double): OrderEntity {
        val user = session.current() ?: error("Sin sesión")
        val subtotal = lines.sumOf { it.product.price * it.quantity }
        val summary = lines.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
        val request = OrderRequest(
            user.email, user.name, lines.map { OrderItemRequest(it.product.id, it.quantity) }, country, shippingType
        )
        val order = try {
            api.createOrder(request).toEntity().copy(itemsSummary = summary)
        } catch (e: Exception) {
            localOrder(user.id, user.email, user.name, summary, lines.sumOf { it.quantity },
                subtotal, shipping, country, shippingType, "COMPRA")
        }
        dao.upsert(order)
        lines.forEach { catalog.decreaseStock(it.product.id, it.quantity) }
        catalog.clearCart()
        // TODO (pendiente): notificaciones push: avisar al cliente y al Administrador del nuevo pedido.
        return order
    }

    /** Solicitud de información de un curso. */
    suspend fun requestCourseInfo(product: ProductEntity): OrderEntity {
        val user = session.current() ?: error("Sin sesión")
        val order = try {
            api.createOrder(OrderRequest(user.email, user.name, listOf(OrderItemRequest(product.id, 1)), "Chile", "DIGITAL", "INFO_CURSO"))
                .toEntity().copy(itemsSummary = "Info: ${product.name}", subtotal = 0.0, total = 0.0)
        } catch (e: Exception) {
            localOrder(user.id, user.email, user.name, "Info: ${product.name}", 1, 0.0, 0.0, "Chile", "DIGITAL", "INFO_CURSO")
        }
        dao.upsert(order)
        // TODO (pendiente): notificaciones push: avisar al Administrador de la solicitud de información.
        return order
    }

    /** El Administrador cambia el estado del pedido. */
    suspend fun updateStatus(order: OrderEntity, status: String) {
        val updated = try {
            if (order.synced) api.updateStatus(order.id, StatusUpdate(status)).toEntity()
                .copy(itemsSummary = order.itemsSummary, userId = order.userId)
            else order.copy(status = status)
        } catch (e: Exception) {
            order.copy(status = status)
        }
        dao.upsert(updated)
        // TODO (pendiente): notificaciones push: avisar al cliente del cambio de estado.
    }

    private fun localOrder(
        userId: Long, email: String, name: String, summary: String, count: Int,
        subtotal: Double, shipping: Double, country: String, type: String, kind: String
    ): OrderEntity {
        val id = System.currentTimeMillis() % 1_000_000_000L + 100_000L
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return OrderEntity(
            id = id, code = "#$year-L${id % 100000}", userId = userId, userEmail = email, userName = name,
            createdAt = System.currentTimeMillis(), itemsSummary = summary, itemsCount = count,
            subtotal = subtotal, shipping = shipping, total = subtotal + shipping,
            status = OrderStatus.PENDIENTE, country = country, shippingType = type, kind = kind, synced = false
        )
    }

    private suspend fun OrderDto.toEntity(): OrderEntity {
        val localUserId = db.userDao().findByEmail(userEmail)?.id ?: 0L
        return OrderEntity(
            id = id, code = code, userId = localUserId, userEmail = userEmail, userName = userName,
            createdAt = createdAt, itemsSummary = items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
            itemsCount = items.sumOf { it.quantity }, subtotal = subtotal, shipping = shipping, total = total,
            status = status, country = country, shippingType = shippingType, kind = kind ?: "COMPRA", synced = true
        )
    }
}
