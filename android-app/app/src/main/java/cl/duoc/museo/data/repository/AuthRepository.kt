package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.util.Security
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val db: AppDatabase, private val session: SessionManager) {

    val currentUser: Flow<UserEntity?> = session.currentUser

    suspend fun login(email: String, password: String, role: Role): Result<UserEntity> {
        val user = db.userDao().findByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("No existe una cuenta con ese correo"))
        if (user.passwordHash != Security.sha256(password))
            return Result.failure(IllegalArgumentException("Contraseña incorrecta"))
        if (user.role != role.name)
            return Result.failure(IllegalArgumentException("Esta cuenta no corresponde al perfil ${role.label}"))
        if (!user.active)
            return Result.failure(IllegalArgumentException("La cuenta está desactivada"))
        session.login(user.id)
        return Result.success(user)
    }

    suspend fun register(name: String, email: String, password: String, avatar: String, interests: String): Result<UserEntity> =
        createUser(name, email, password, Role.CLIENTE, avatar, interests).onSuccess { session.login(it.id) }

    /** También la usa el Administrador para mantener cuentas ficticias de demostración. */
    suspend fun createUser(
        name: String, email: String, password: String, role: Role,
        avatar: String = "avatar_1", interests: String = ""
    ): Result<UserEntity> {
        if (db.userDao().findByEmail(email.trim()) != null)
            return Result.failure(IllegalArgumentException("Ese correo ya está registrado"))
        val user = UserEntity(
            name = name.trim(), email = email.trim().lowercase(),
            passwordHash = Security.sha256(password), role = role.name,
            avatar = avatar, interests = interests
        )
        val id = db.userDao().insert(user)
        return Result.success(user.copy(id = id))
    }

    suspend fun update(user: UserEntity) = db.userDao().update(user)

    suspend fun changePassword(user: UserEntity, current: String, new: String): Result<Unit> {
        if (user.passwordHash != Security.sha256(current))
            return Result.failure(IllegalArgumentException("La contraseña actual no coincide"))
        db.userDao().update(user.copy(passwordHash = Security.sha256(new)))
        return Result.success(Unit)
    }

    fun observeAllUsers(): Flow<List<UserEntity>> = db.userDao().observeAll()

    suspend fun setActive(user: UserEntity, active: Boolean) = db.userDao().update(user.copy(active = active))

    suspend fun deleteUser(user: UserEntity) = db.userDao().delete(user)

    fun logout() = session.logout()
}
