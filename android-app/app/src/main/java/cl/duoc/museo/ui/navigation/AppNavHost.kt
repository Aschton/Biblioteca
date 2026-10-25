package cl.duoc.museo.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.roleEnum
import cl.duoc.museo.ui.screens.AdminUsersScreen
import cl.duoc.museo.ui.screens.CartScreen
import cl.duoc.museo.ui.screens.CatalogScreen
import cl.duoc.museo.ui.screens.DashboardScreen
import cl.duoc.museo.ui.screens.FavoritesScreen
import cl.duoc.museo.ui.screens.LoginScreen
import cl.duoc.museo.ui.screens.MapScreen
import cl.duoc.museo.ui.screens.MuseumDetailScreen
import cl.duoc.museo.ui.screens.MuseumFormScreen
import cl.duoc.museo.ui.screens.NotificationsScreen
import cl.duoc.museo.ui.screens.OrdersScreen
import cl.duoc.museo.ui.screens.ProductDetailScreen
import cl.duoc.museo.ui.screens.ProductFormScreen
import cl.duoc.museo.ui.screens.ProfileScreen
import cl.duoc.museo.ui.screens.QrResultScreen
import cl.duoc.museo.ui.screens.QrScannerScreen
import cl.duoc.museo.ui.screens.RegisterScreen
import cl.duoc.museo.ui.screens.WelcomeScreen
import cl.duoc.museo.ui.theme.GoldLight
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.SessionViewModel

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MAP = "map"
    const val MUSEUM = "museum/{id}"
    const val MUSEUM_FORM = "museum_form/{id}"
    const val QR_SCANNER = "qr_scanner"
    const val QR_RESULT = "qr_result/{code}"
    const val CATALOG = "catalog"
    const val PRODUCT = "product/{id}"
    const val PRODUCT_FORM = "product_form/{id}"
    const val FAVORITES = "favorites"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val ADMIN_USERS = "admin_users"

    fun museum(id: Long) = "museum/$id"
    fun museumForm(id: Long = -1) = "museum_form/$id"
    fun product(id: Long) = "product/$id"
    fun productForm(id: Long = -1) = "product_form/$id"
    fun qrResult(code: String) = "qr_result/${Uri.encode(code)}"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Inicio", Icons.Filled.Home),
    BottomItem(Routes.MAP, "Mapa", Icons.Filled.Map),
    BottomItem(Routes.CATALOG, "Catálogo", Icons.AutoMirrored.Filled.MenuBook),
    BottomItem(Routes.NOTIFICATIONS, "Avisos", Icons.Filled.Notifications),
    BottomItem(Routes.PROFILE, "Perfil", Icons.Filled.Person)
)

/** Navega a una pestaña de la barra inferior sin apilar pantallas repetidas. */
fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val sessionVm: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val userState by sessionVm.userState.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val user = userState.user
    val role = user?.roleEnum

    // TODO (pendiente): pedir el permiso POST_NOTIFICATIONS (Android 13+) al iniciar sesión.

    // Si se cierra sesión desde cualquier pantalla, volver a la bienvenida
    LaunchedEffect(userState) {
        if (!userState.loading && user == null && currentRoute != null &&
            currentRoute !in listOf(Routes.WELCOME, Routes.LOGIN, Routes.REGISTER)
        ) {
            nav.navigate(Routes.WELCOME) { popUpTo(0) { inclusive = true } }
        }
    }

    val start = remember { if (sessionVm.hasSession()) Routes.HOME else Routes.WELCOME }
    val showBottom = user != null && bottomItems.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottom) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                bottomItems.forEach { item ->
                    // El Investigador no tiene catálogo comercial
                    if (item.route == Routes.CATALOG && role == Role.INVESTIGADOR) return@forEach
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { nav.navigateTab(item.route) },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Navy, selectedTextColor = Navy, indicatorColor = GoldLight
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            val goHome: () -> Unit = { nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } } }
            val back: () -> Unit = { nav.popBackStack() }
            val idArg = listOf(navArgument("id") { type = NavType.LongType })

            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onStart = { nav.navigate(Routes.LOGIN) },
                    onRegister = { nav.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(onBack = back, onLoggedIn = goHome, onRegister = { nav.navigate(Routes.REGISTER) })
            }
            composable(Routes.REGISTER) {
                RegisterScreen(onBack = back, onRegistered = goHome, onLogin = { nav.navigate(Routes.LOGIN) })
            }
            composable(Routes.HOME) {
                DashboardScreen(user = user, onNavigate = { nav.navigate(it) }, onTab = { nav.navigateTab(it) })
            }
            composable(Routes.MAP) {
                MapScreen(onOpenMuseum = { nav.navigate(Routes.museum(it)) }, role = role,
                    onNewMuseum = { nav.navigate(Routes.museumForm()) })
            }
            composable(Routes.MUSEUM, arguments = idArg) {
                MuseumDetailScreen(role = role, onBack = back,
                    onEdit = { nav.navigate(Routes.museumForm(it)) })
            }
            composable(Routes.MUSEUM_FORM, arguments = idArg) {
                MuseumFormScreen(onBack = back)
            }
            composable(Routes.QR_SCANNER) {
                QrScannerScreen(onBack = back, onCode = { code ->
                    nav.navigate(Routes.qrResult(code)) { popUpTo(Routes.QR_SCANNER) { inclusive = true } }
                })
            }
            composable(Routes.QR_RESULT, arguments = listOf(navArgument("code") { type = NavType.StringType })) {
                QrResultScreen(
                    onBack = back,
                    onOpen = { route -> nav.navigate(route) { popUpTo(Routes.QR_RESULT) { inclusive = true } } },
                    onScanAgain = { nav.navigate(Routes.QR_SCANNER) { popUpTo(Routes.QR_RESULT) { inclusive = true } } }
                )
            }
            composable(Routes.CATALOG) {
                CatalogScreen(role = role,
                    onOpen = { nav.navigate(Routes.product(it)) },
                    onCart = { nav.navigate(Routes.CART) },
                    onNew = { nav.navigate(Routes.productForm()) },
                    onEdit = { nav.navigate(Routes.productForm(it)) },
                    cartCount = sessionVm.cartCount)
            }
            composable(Routes.PRODUCT, arguments = idArg) {
                ProductDetailScreen(role = role, onBack = back, onCart = { nav.navigate(Routes.CART) },
                    onEdit = { nav.navigate(Routes.productForm(it)) })
            }
            composable(Routes.PRODUCT_FORM, arguments = idArg) { ProductFormScreen(onBack = back) }
            composable(Routes.FAVORITES) {
                FavoritesScreen(onBack = back, onMuseum = { nav.navigate(Routes.museum(it)) },
                    onProduct = { nav.navigate(Routes.product(it)) })
            }
            composable(Routes.CART) {
                CartScreen(onBack = back, onOrders = {
                    nav.navigate(Routes.ORDERS) { popUpTo(Routes.CART) { inclusive = true } }
                }, onCatalog = { nav.navigateTab(Routes.CATALOG) })
            }
            composable(Routes.ORDERS) { OrdersScreen(onBack = back) }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(role = role)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onNavigate = { nav.navigate(it) })
            }
            composable(Routes.ADMIN_USERS) { AdminUsersScreen(onBack = back) }
        }
    }
}
