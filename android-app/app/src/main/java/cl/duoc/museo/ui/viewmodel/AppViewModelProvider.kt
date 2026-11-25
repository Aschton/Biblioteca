package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.museo.AppContainer
import cl.duoc.museo.MuseoApp

/** Fábrica única de ViewModels (MVVM con inyección manual desde AppContainer). */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SessionViewModel(c().session, c().authRepository, c().notificationRepository, c().catalogRepository) }
        initializer { AuthViewModel(c().authRepository) }
        initializer { DashboardViewModel(c().museumRepository, c().catalogRepository, c().orderRepository, c().notificationRepository) }
        initializer { MapViewModel(c().museumRepository, c().session) }
        initializer { MuseumDetailViewModel(createSavedStateHandle(), c().museumRepository, c().weatherRepository) }
        initializer { MuseumFormViewModel(createSavedStateHandle(), c().museumRepository, c().notificationRepository) }
        initializer { QrResultViewModel(createSavedStateHandle(), c().qrRepository) }
        initializer { CatalogViewModel(c().catalogRepository, c().session) }
        initializer { ProductDetailViewModel(createSavedStateHandle(), c().catalogRepository, c().orderRepository) }
        initializer { ProductFormViewModel(createSavedStateHandle(), c().catalogRepository, c().notificationRepository) }
        initializer { FavoritesViewModel(c().museumRepository, c().catalogRepository, c().qrRepository) }
        initializer { CartViewModel(c().catalogRepository, c().orderRepository) }
        initializer { OrdersViewModel(c().orderRepository, c().session) }
        initializer { NotificationsViewModel(c().notificationRepository) }
        initializer { ProfileViewModel(c().authRepository, c().session) }
        initializer { AdminUsersViewModel(c().authRepository) }
    }
}

private fun CreationExtras.c(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MuseoApp).container
