package cl.duoc.museo

import android.content.Context
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.remote.RetrofitClient
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.data.repository.QrRepository

/** Inyección de dependencias manual (sin Hilt para mantener el MVP simple). */
class AppContainer(context: Context) {
    val database = AppDatabase.build(context)
    private val api = RetrofitClient.api
    val session = SessionManager(context, database.userDao())

    val authRepository = AuthRepository(database, session)
    val museumRepository = MuseumRepository(database, api, session)
    val catalogRepository = CatalogRepository(database, api, session)
    val orderRepository = OrderRepository(database, api, session, catalogRepository)
    val qrRepository = QrRepository(database, api, session)
}
