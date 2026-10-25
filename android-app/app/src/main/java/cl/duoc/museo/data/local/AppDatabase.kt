package cl.duoc.museo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        MuseumEntity::class,
        ProductEntity::class,
        FavoriteEntity::class,
        CartItemEntity::class,
        QrHistoryEntity::class,
        MuseumViewEntity::class,
        OrderEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun museumDao(): MuseumDao
    abstract fun productDao(): ProductDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cartDao(): CartDao
    abstract fun historyDao(): HistoryDao
    abstract fun orderDao(): OrderDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "museo_de_los_museos.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
