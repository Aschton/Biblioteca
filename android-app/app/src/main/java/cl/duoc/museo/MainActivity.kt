package cl.duoc.museo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.ui.navigation.AppNavHost
import cl.duoc.museo.ui.theme.MuseoTheme
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val sessionVm: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by sessionVm.userState.collectAsStateWithLifecycle()

            MuseoTheme(
                largeText = state.user?.largeText == true,
                highContrast = state.user?.highContrast == true
            ) {
                // TODO (pendiente): abrir la pantalla indicada al tocar una notificación push.
                AppNavHost()
            }
        }
    }
}
