package cl.duoc.museo

import android.content.Context
import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.remote.RetrofitClient
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.data.repository.OrderRepository
import cl.duoc.museo.data.repository.QrRepository
import cl.duoc.museo.data.repository.WeatherRepository
import cl.duoc.museo.notifications.NotificationHelper

/** Inyección de dependencias manual (sin Hilt para mantener el MVP simple). */
class AppContainer(context: Context) {
    val database = AppDatabase.build(context)
    val session = SessionManager(context, database.userDao())
    val notificationHelper = NotificationHelper(context)

    val authRepository = AuthRepository(database, session)
    // Cada repositorio habla con su microservicio
    val museumRepository = MuseumRepository(database, RetrofitClient.museos, session)
    val catalogRepository = CatalogRepository(database, RetrofitClient.catalogo, session)
    val notificationRepository = NotificationRepository(database, RetrofitClient.museos, session, notificationHelper)
    val orderRepository = OrderRepository(database, RetrofitClient.pedidos, session, catalogRepository, notificationRepository)
    val qrRepository = QrRepository(database, RetrofitClient.museos, RetrofitClient.catalogo, session)

    // API externa (Open-Meteo), independiente de los microservicios propios
    val weatherRepository = WeatherRepository(RetrofitClient.openMeteo)
}
