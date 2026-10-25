package cl.duoc.museo.data

import android.content.Context
import cl.duoc.museo.data.local.UserDao
import cl.duoc.museo.data.local.UserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Guarda qué usuario está activo en el teléfono. Todos los datos personales
 * (favoritos, carrito, historial) se filtran por este id,
 * así al cambiar de perfil no se mezcla la información.
 */
class SessionManager(context: Context, private val userDao: UserDao) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val _userId = MutableStateFlow(prefs.getLong(KEY, -1L).takeIf { it > 0 })
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = _userId.flatMapLatest { id ->
        if (id == null) flowOf(null) else userDao.observeById(id)
    }

    fun currentId(): Long? = _userId.value

    suspend fun current(): UserEntity? = _userId.value?.let { userDao.getById(it) }

    fun login(id: Long) {
        prefs.edit().putLong(KEY, id).apply()
        _userId.value = id
    }

    fun logout() {
        prefs.edit().remove(KEY).apply()
        _userId.value = null
    }

    private companion object { const val KEY = "current_user_id" }
}
