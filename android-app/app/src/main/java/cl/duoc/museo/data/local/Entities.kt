package cl.duoc.museo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Roles disponibles en la app. */
enum class Role(val label: String) {
    ADMIN("Administrador"),
    INVESTIGADOR("Investigador"),
    CLIENTE("Cliente");
}

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String,                 // Role.name
    val avatar: String = "avatar_1",  // "avatar_N" predefinido o ruta a foto tomada con la cámara
    val active: Boolean = true,
    // Preferencias (pantalla 15)
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val language: String = "Español",
    val shippingAddress: String = "",
    val interests: String = ""
)

val UserEntity.roleEnum: Role get() = runCatching { Role.valueOf(role) }.getOrDefault(Role.CLIENTE)

/** Caché local de museos virtuales (fuente oficial: backend). */
@Entity(tableName = "museums")
data class MuseumEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val imageUrl: String = "",
    val classification: String,
    val project: String,
    val virtualTourUrl: String,
    val rating: Double = 0.0,
    val published: Boolean = true,
    val qrCode: String,
    val createdBy: Long? = null,
    val pendingSync: Boolean = false
)

/** Caché local del catálogo de productos y cursos. */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val category: String,   // CURSO, LIBRO, MATERIAL, ARTICULO
    val description: String,
    val price: Double,
    val imageUrl: String = "",
    val stock: Int,
    val active: Boolean = true,
    val qrCode: String,
    val rating: Double = 0.0
)

val ProductEntity.isCourse: Boolean get() = category == "CURSO"

/** Favoritos separados por usuario. itemType: MUSEUM o PRODUCT. */
@Entity(tableName = "favorites", primaryKeys = ["userId", "itemType", "itemId"])
data class FavoriteEntity(
    val userId: Long,
    val itemType: String,
    val itemId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/** Carrito separado por usuario. */
@Entity(tableName = "cart_items", primaryKeys = ["userId", "productId"])
data class CartItemEntity(
    val userId: Long,
    val productId: Long,
    val quantity: Int
)

/** Historial de lecturas QR. El índice único evita duplicados por usuario. */
@Entity(
    tableName = "qr_history",
    indices = [Index(value = ["userId", "code"], unique = true)]
)
data class QrHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val code: String,
    val itemType: String,
    val itemId: Long,
    val title: String,
    val scannedAt: Long = System.currentTimeMillis()
)

/** Historial de museos consultados (para filtro en favoritos). */
@Entity(tableName = "museum_views", primaryKeys = ["userId", "museumId"])
data class MuseumViewEntity(
    val userId: Long,
    val museumId: Long,
    val viewedAt: Long = System.currentTimeMillis()
)

/** Historial local de pedidos/solicitudes (fuente oficial: backend). */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: Long,
    val code: String,
    val userId: Long,
    val userEmail: String,
    val userName: String,
    val createdAt: Long,
    val itemsSummary: String,
    val itemsCount: Int,
    val subtotal: Double,
    val shipping: Double,
    val total: Double,
    val status: String,      // PENDIENTE, CONFIRMADO, ENVIADO, ENTREGADO
    val country: String,
    val shippingType: String,
    val kind: String = "COMPRA", // COMPRA o INFO_CURSO
    val synced: Boolean = true
)
