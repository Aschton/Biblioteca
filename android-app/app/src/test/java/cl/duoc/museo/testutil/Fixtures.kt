package cl.duoc.museo.testutil

import cl.duoc.museo.data.local.CartLine
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.NotificationEntity
import cl.duoc.museo.data.local.NotificationItem
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.remote.MuseumDto
import cl.duoc.museo.data.remote.OrderDto
import cl.duoc.museo.data.remote.OrderItemDto
import cl.duoc.museo.data.remote.ProductDto
import cl.duoc.museo.util.Security

/** Datos de prueba reutilizables. */
object Fixtures {

    fun museum(
        id: Long = 1,
        published: Boolean = true,
        pendingSync: Boolean = false,
        country: String = "Chile",
        project: String = "Red Iberoamericana",
        rating: Double = 4.5,
        createdBy: Long? = null
    ) = MuseumEntity(
        id = id, name = "Museo $id", country = country, city = "Santiago",
        latitude = -33.43, longitude = -70.64,
        description = "Descripción del museo de prueba número $id",
        imageUrl = "", classification = "Historia", project = project,
        virtualTourUrl = "https://tour.cl/$id", rating = rating, published = published,
        qrCode = "MUSEO:$id", createdBy = createdBy, pendingSync = pendingSync
    )

    fun museumDto(id: Long? = 1, name: String = "Museo remoto") = MuseumDto(
        id = id, name = name, country = "Perú", city = "Lima", latitude = -12.06, longitude = -77.03,
        description = "Ficha que viene del microservicio", imageUrl = "https://img.cl/a.jpg",
        classification = "Historia", project = "Red Iberoamericana", virtualTourUrl = "https://tour.pe",
        rating = 4.7, published = true, qrCode = if (id == null) null else "MUSEO:$id"
    )

    fun product(
        id: Long = 1,
        category: String = "LIBRO",
        price: Double = 10.0,
        stock: Int = 5,
        active: Boolean = true
    ) = ProductEntity(
        id = id, name = "Producto $id", category = category, description = "Descripción del producto $id",
        price = price, imageUrl = "", stock = stock, active = active, qrCode = "PROD:$id", rating = 4.0
    )

    fun productDto(id: Long? = 1) = ProductDto(
        id = id, name = "Libro remoto", category = "LIBRO", description = "Viene de ms-catalogo",
        price = 20.0, imageUrl = "https://img.cl/b.jpg", stock = 3, active = true,
        qrCode = if (id == null) null else "PROD:$id", rating = 4.2
    )

    fun user(
        id: Long = 1,
        role: Role = Role.CLIENTE,
        password: String = "Cliente123",
        active: Boolean = true,
        notifications: Boolean = true
    ) = UserEntity(
        id = id, name = "Usuario $id", email = "user$id@museo.cl", passwordHash = Security.sha256(password),
        role = role.name, active = active, notificationsEnabled = notifications
    )

    fun order(
        id: Long = 1,
        status: String = "PENDIENTE",
        synced: Boolean = true,
        userId: Long = 1,
        kind: String = "COMPRA"
    ) = OrderEntity(
        id = id, code = "#2026-0012$id", userId = userId, userEmail = "user$userId@museo.cl", userName = "Usuario $userId",
        createdAt = 1_700_000_000_000L, itemsSummary = "1x Producto 1", itemsCount = 1,
        subtotal = 10.0, shipping = 4.0, total = 14.0, status = status,
        country = "Chile", shippingType = "ESTANDAR", kind = kind, synced = synced
    )

    fun orderDto(id: Long = 1, status: String = "PENDIENTE", email: String = "user1@museo.cl", kind: String? = "COMPRA") = OrderDto(
        id = id, code = "#2026-0012$id", userEmail = email, userName = "Usuario 1", createdAt = 1_700_000_000_000L,
        items = listOf(OrderItemDto(1, "Producto 1", 2, 10.0)),
        subtotal = 20.0, shipping = 5.0, total = 25.0, status = status,
        country = "Chile", shippingType = "ESTANDAR", kind = kind
    )

    fun cartLine(product: ProductEntity = product(), quantity: Int = 1) = CartLine(product, quantity)

    fun notification(id: Long = 1, category: String = "PEDIDO", read: Boolean = false) =
        NotificationItem(NotificationEntity(id = id, title = "Aviso $id", body = "Cuerpo", category = category), read)
}
