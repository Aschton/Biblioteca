package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY role, name")
    fun observeAll(): Flow<List<UserEntity>>
}

@Dao
interface MuseumDao {
    @Query("SELECT * FROM museums WHERE published = 1 OR :includeUnpublished ORDER BY name")
    fun observeAll(includeUnpublished: Boolean): Flow<List<MuseumEntity>>

    @Query("SELECT * FROM museums WHERE id = :id")
    fun observeById(id: Long): Flow<MuseumEntity?>

    @Query("SELECT * FROM museums WHERE id = :id")
    suspend fun getById(id: Long): MuseumEntity?

    @Query("SELECT * FROM museums WHERE qrCode = :code LIMIT 1")
    suspend fun getByQr(code: String): MuseumEntity?

    @Query("SELECT * FROM museums WHERE pendingSync = 1")
    suspend fun pendingSync(): List<MuseumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(museum: MuseumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(museums: List<MuseumEntity>)

    @Query("DELETE FROM museums WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 OR :includeInactive ORDER BY category, name")
    fun observeAll(includeInactive: Boolean): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE qrCode = :code LIMIT 1")
    suspend fun getByQr(code: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)
}

@Dao
interface FavoriteDao {
    @Query(
        "SELECT m.* FROM favorites f INNER JOIN museums m ON m.id = f.itemId " +
            "WHERE f.userId = :userId AND f.itemType = 'MUSEUM' ORDER BY f.createdAt DESC"
    )
    fun observeMuseums(userId: Long): Flow<List<MuseumEntity>>

    @Query(
        "SELECT p.* FROM favorites f INNER JOIN products p ON p.id = f.itemId " +
            "WHERE f.userId = :userId AND f.itemType = 'PRODUCT' ORDER BY f.createdAt DESC"
    )
    fun observeProducts(userId: Long): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE userId = :userId AND itemType = :type AND itemId = :itemId")
    fun observeIsFavorite(userId: Long, type: String, itemId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND itemType = :type AND itemId = :itemId")
    suspend fun delete(userId: Long, type: String, itemId: Long)
}

data class CartLine(
    @Embedded val product: ProductEntity,
    val quantity: Int
)

@Dao
interface CartDao {
    @Query(
        "SELECT p.*, c.quantity AS quantity FROM cart_items c " +
            "INNER JOIN products p ON p.id = c.productId WHERE c.userId = :userId"
    )
    fun observeCart(userId: Long): Flow<List<CartLine>>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE userId = :userId")
    fun observeCount(userId: Long): Flow<Int>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun get(userId: Long, productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: Long, productId: Long)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clear(userId: Long)
}

@Dao
interface HistoryDao {
    /** REPLACE sobre el índice único (userId, code): no se duplican lecturas. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQr(entry: QrHistoryEntity)

    @Query("SELECT * FROM qr_history WHERE userId = :userId ORDER BY scannedAt DESC")
    fun observeQr(userId: Long): Flow<List<QrHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertView(view: MuseumViewEntity)

    @Query(
        "SELECT m.* FROM museum_views v INNER JOIN museums m ON m.id = v.museumId " +
            "WHERE v.userId = :userId ORDER BY v.viewedAt DESC"
    )
    fun observeViewedMuseums(userId: Long): Flow<List<MuseumEntity>>
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE synced = 0")
    suspend fun unsynced(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun count(): Int
}
