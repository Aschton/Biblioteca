package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.QrHistoryEntity
import cl.duoc.museo.data.remote.ApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Resultado de un QR válido. type = MUSEUM o PRODUCT. */
data class QrResult(
    val code: String,
    val type: String,
    val id: Long,
    val title: String,
    val description: String,
    val label: String,
    val imageUrl: String = ""
) {
    val route: String get() = if (type == "MUSEUM") "museum/$id" else "product/$id"
}

/**
 * Formato de los códigos: "MUSEO:<id>" o "PROD:<id>".
 * Se valida localmente (caché SQLite) y, si no se encuentra, se consulta al backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QrRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    private val session: SessionManager
) {
    fun observeHistory(): Flow<List<QrHistoryEntity>> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.historyDao().observeQr(id)
    }

    suspend fun resolve(raw: String): Result<QrResult> {
        val code = raw.trim()
        if (!code.startsWith("MUSEO:") && !code.startsWith("PROD:"))
            return Result.failure(IllegalArgumentException("El código no pertenece a Museo de los Museos"))

        val local: QrResult? = if (code.startsWith("MUSEO:")) {
            db.museumDao().getByQr(code)?.let {
                QrResult(code, "MUSEUM", it.id, it.name, it.description, "Museo virtual · ${it.country}", it.imageUrl)
            }
        } else {
            db.productDao().getByQr(code)?.let {
                val label = when (it.category) {
                    "CURSO" -> "Curso"; "LIBRO" -> "Publicación"; "MATERIAL" -> "Material"; else -> "Producto"
                }
                QrResult(code, "PRODUCT", it.id, it.name, it.description, label, it.imageUrl)
            }
        }

        val result = local ?: runCatching {
            val r = api.resolveQr(code)
            QrResult(r.code, r.type, r.id, r.title, r.description, if (r.type == "MUSEUM") "Museo virtual" else "Producto")
        }.getOrNull()
        ?: return Result.failure(IllegalArgumentException("Código desconocido: $code"))

        session.currentId()?.let { uid ->
            db.historyDao().upsertQr(QrHistoryEntity(userId = uid, code = code, itemType = result.type, itemId = result.id, title = result.title))
        }
        return Result.success(result)
    }
}
