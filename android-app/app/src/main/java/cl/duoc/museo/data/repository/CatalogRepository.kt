package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.CartItemEntity
import cl.duoc.museo.data.local.CartLine
import cl.duoc.museo.data.local.FavoriteEntity
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.remote.ApiService
import cl.duoc.museo.data.remote.toDto
import cl.duoc.museo.data.remote.toEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class Availability(val available: Boolean, val stock: Int, val checkedOnline: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    private val session: SessionManager
) {
    private val dao = db.productDao()

    fun observeProducts(includeInactive: Boolean): Flow<List<ProductEntity>> = dao.observeAll(includeInactive)
    fun observeProduct(id: Long): Flow<ProductEntity?> = dao.observeById(id)
    suspend fun getProduct(id: Long) = dao.getById(id)

    /** Sincroniza el catálogo vía Retrofit; si falla se usa la caché SQLite. */
    suspend fun refresh(): Boolean = runCatching {
        dao.upsertAll(api.products().map { it.toEntity() })
    }.isSuccess

    /** Valida disponibilidad con el backend antes de confirmar (fallback: stock local). */
    suspend fun checkAvailability(productId: Long, qty: Int): Availability = try {
        val r = api.availability(productId, qty)
        Availability(r.available, r.stock, true)
    } catch (e: Exception) {
        val p = dao.getById(productId)
        Availability(p != null && p.active && p.stock >= qty, p?.stock ?: 0, false)
    }

    // ----- Administración (alta, modificación, desactivación) -----

    suspend fun save(product: ProductEntity, isNew: Boolean): Boolean = try {
        val saved = if (isNew) api.createProduct(product.toDto(sendId = false))
        else api.updateProduct(product.id, product.toDto())
        dao.upsert(saved.toEntity().copy(imageUrl = product.imageUrl))
        true
    } catch (e: Exception) {
        val local = if (isNew) {
            val id = System.currentTimeMillis() % 1_000_000_000L + 1000L
            product.copy(id = id, qrCode = "PROD:$id")
        } else product
        dao.upsert(local)
        false
    }

    suspend fun setActive(product: ProductEntity, active: Boolean): Boolean {
        if (!active) runCatching { api.deactivateProduct(product.id) }
        return save(product.copy(active = active), false)
    }

    suspend fun decreaseStock(productId: Long, qty: Int) {
        dao.getById(productId)?.let { dao.upsert(it.copy(stock = (it.stock - qty).coerceAtLeast(0))) }
    }

    // ----- Favoritos de recursos -----

    fun observeFavoriteProducts(): Flow<List<ProductEntity>> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.favoriteDao().observeProducts(id)
    }

    fun observeIsFavorite(productId: Long): Flow<Boolean> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(false) else db.favoriteDao().observeIsFavorite(id, TYPE, productId)
    }

    suspend fun setFavorite(productId: Long, favorite: Boolean) {
        val userId = session.currentId() ?: return
        if (favorite) db.favoriteDao().insert(FavoriteEntity(userId, TYPE, productId))
        else db.favoriteDao().delete(userId, TYPE, productId)
    }

    // ----- Carrito (por usuario) -----

    fun observeCart(): Flow<List<CartLine>> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.cartDao().observeCart(id)
    }

    fun observeCartCount(): Flow<Int> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(0) else db.cartDao().observeCount(id)
    }

    suspend fun addToCart(productId: Long, qty: Int) {
        val userId = session.currentId() ?: return
        val current = db.cartDao().get(userId, productId)?.quantity ?: 0
        db.cartDao().upsert(CartItemEntity(userId, productId, current + qty))
    }

    suspend fun setQuantity(productId: Long, qty: Int) {
        val userId = session.currentId() ?: return
        if (qty <= 0) db.cartDao().delete(userId, productId)
        else db.cartDao().upsert(CartItemEntity(userId, productId, qty))
    }

    suspend fun clearCart() {
        session.currentId()?.let { db.cartDao().clear(it) }
    }

    companion object { const val TYPE = "PRODUCT" }
}
