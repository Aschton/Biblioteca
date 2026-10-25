package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.FavoriteEntity
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.MuseumViewEntity
import cl.duoc.museo.data.remote.ApiService
import cl.duoc.museo.data.remote.toDto
import cl.duoc.museo.data.remote.toEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Museos virtuales: el backend es la fuente oficial y Room actúa como caché.
 * Si el backend no responde, la app sigue funcionando con la caché local.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MuseumRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    private val session: SessionManager
) {
    private val dao = db.museumDao()

    fun observeMuseums(includeUnpublished: Boolean): Flow<List<MuseumEntity>> = dao.observeAll(includeUnpublished)

    fun observeMuseum(id: Long): Flow<MuseumEntity?> = dao.observeById(id)

    suspend fun getMuseum(id: Long): MuseumEntity? = dao.getById(id)

    /** @return true si se sincronizó con el backend. */
    suspend fun refresh(): Boolean = runCatching {
        pushPending()
        val remote = api.museums().map { it.toEntity() }
        dao.upsertAll(remote)
    }.isSuccess

    private suspend fun pushPending() {
        dao.pendingSync().forEach { local ->
            val created = api.createMuseum(local.toDto(sendId = false)).toEntity()
            dao.deleteById(local.id)
            dao.upsert(created.copy(createdBy = local.createdBy))
        }
    }

    /** Crea o actualiza una ficha (Investigador). Devuelve true si quedó en el backend. */
    suspend fun save(museum: MuseumEntity, isNew: Boolean): Boolean {
        val userId = session.currentId()
        return try {
            val saved = if (isNew) api.createMuseum(museum.toDto(sendId = false))
            else api.updateMuseum(museum.id, museum.toDto())
            dao.upsert(saved.toEntity().copy(createdBy = museum.createdBy ?: userId, imageUrl = museum.imageUrl))
            true
        } catch (e: Exception) {
            val local = if (isNew) {
                val id = System.currentTimeMillis() % 1_000_000_000L + 1000L
                museum.copy(id = id, qrCode = "MUSEO:$id", createdBy = userId, pendingSync = true)
            } else museum
            dao.upsert(local)
            false
        }
    }

    suspend fun setPublished(museum: MuseumEntity, published: Boolean): Boolean = save(museum.copy(published = published), false)

    // ----- Favoritos y consultados (por usuario) -----

    fun observeFavorites(): Flow<List<MuseumEntity>> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.favoriteDao().observeMuseums(id)
    }

    fun observeViewed(): Flow<List<MuseumEntity>> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.historyDao().observeViewedMuseums(id)
    }

    fun observeIsFavorite(museumId: Long): Flow<Boolean> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(false) else db.favoriteDao().observeIsFavorite(id, TYPE, museumId)
    }

    suspend fun setFavorite(museumId: Long, favorite: Boolean) {
        val userId = session.currentId() ?: return
        if (favorite) db.favoriteDao().insert(FavoriteEntity(userId, TYPE, museumId))
        else db.favoriteDao().delete(userId, TYPE, museumId)
    }

    suspend fun recordView(museumId: Long) {
        val userId = session.currentId() ?: return
        db.historyDao().upsertView(MuseumViewEntity(userId, museumId))
    }

    companion object { const val TYPE = "MUSEUM" }
}
