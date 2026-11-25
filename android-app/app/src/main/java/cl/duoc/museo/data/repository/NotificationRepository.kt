package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.NotificationEntity
import cl.duoc.museo.data.local.NotificationItem
import cl.duoc.museo.data.local.NotificationReadEntity
import cl.duoc.museo.data.remote.MuseosApi
import cl.duoc.museo.data.remote.NoticeDto
import cl.duoc.museo.notifications.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepository(
    private val db: AppDatabase,
    private val api: MuseosApi,
    private val session: SessionManager,
    private val helper: NotificationHelper
) {
    private val dao = db.notificationDao()

    fun observeMine(): Flow<List<NotificationItem>> = session.currentUser.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else dao.observeFor(u.id, u.role)
    }

    fun observeUnread(): Flow<Int> = session.currentUser.flatMapLatest { u ->
        if (u == null) flowOf(0) else dao.observeUnread(u.id, u.role)
    }

    suspend fun markRead(id: Long) {
        session.currentId()?.let { dao.markRead(NotificationReadEntity(it, id)) }
    }

    suspend fun markAllRead() {
        session.current()?.let { dao.markAllRead(it.id, it.role) }
    }

    /**
     * Crea un aviso, lo guarda en SQLite y lanza la notificación push local.
     * @param targetUserId si no es null, solo lo verá ese usuario (ej: estado de su pedido).
     */
    suspend fun publish(
        title: String, body: String, category: String,
        targetRole: String = "ALL", targetUserId: Long? = null,
        destination: String = "", sendToBackend: Boolean = false
    ) {
        val entity = NotificationEntity(
            title = title, body = body, category = category,
            targetRole = targetRole, targetUserId = targetUserId, destination = destination
        )
        val id = dao.insert(entity)
        if (sendToBackend) runCatching {
            api.createNotice(NoticeDto(title = title, body = body, category = category, targetRole = targetRole, destination = destination, createdAt = entity.createdAt))
        }
        val recipientAllows = targetUserId?.let { db.userDao().getById(it)?.notificationsEnabled } ?: true
        val currentAllows = session.current()?.notificationsEnabled ?: true
        if (recipientAllows && currentAllows) helper.show(id, title, body, destination)
    }

    /** Trae avisos publicados en el backend que aún no estén en SQLite. */
    suspend fun refresh(): Boolean = runCatching {
        api.notices().forEach { n ->
            val created = n.createdAt ?: return@forEach
            if (!dao.exists(n.title, created)) {
                dao.insert(
                    NotificationEntity(
                        title = n.title, body = n.body, category = n.category,
                        targetRole = n.targetRole, destination = n.destination ?: "", createdAt = created
                    )
                )
            }
        }
    }.isSuccess
}
