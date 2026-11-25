package cl.duoc.museo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.notifications.NotificationHelper
import cl.duoc.museo.ui.navigation.AppNavHost
import cl.duoc.museo.ui.theme.MuseoTheme
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {

    /** Ruta a abrir cuando el usuario toca una notificación push. */
    private val pendingDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDestination.value = intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION)

        setContent {
            val sessionVm: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by sessionVm.userState.collectAsStateWithLifecycle()
            val destination by pendingDestination

            MuseoTheme(
                largeText = state.user?.largeText == true,
                highContrast = state.user?.highContrast == true
            ) {
                AppNavHost(
                    pendingDestination = destination,
                    onDestinationHandled = { pendingDestination.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION)?.let { pendingDestination.value = it }
    }
}
