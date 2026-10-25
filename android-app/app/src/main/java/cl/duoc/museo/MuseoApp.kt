package cl.duoc.museo

import android.app.Application
import cl.duoc.museo.data.local.DemoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class MuseoApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // TODO (pendiente): crear el canal de notificaciones push.

        // osmdroid exige un user agent propio para descargar teselas de OpenStreetMap
        Configuration.getInstance().apply {
            load(this@MuseoApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        // Carga datos ficticios la primera vez (perfiles de prueba, museos, catálogo)
        appScope.launch { DemoData.seedIfEmpty(container.database) }
    }
}
