package cl.duoc.museo.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.UserEntity
import cl.duoc.museo.data.local.roleEnum
import cl.duoc.museo.ui.components.Avatar
import cl.duoc.museo.ui.components.OfflineBanner
import cl.duoc.museo.ui.components.Rating
import cl.duoc.museo.ui.components.ReferenceImage
import cl.duoc.museo.ui.components.SectionTitle
import cl.duoc.museo.ui.components.Tag
import cl.duoc.museo.ui.navigation.Routes
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.theme.GoldLight
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.theme.NavyLight
import cl.duoc.museo.ui.theme.Teal
import cl.duoc.museo.ui.theme.TealLight
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.DashboardViewModel
import cl.duoc.museo.ui.viewmodel.SessionViewModel
import cl.duoc.museo.util.Format

private data class QuickAction(val label: String, val icon: ImageVector, val route: String, val color: Color)

// =============== 4. Principal / Dashboard (adaptado al rol) ===============

@Composable
fun DashboardScreen(user: UserEntity?, onNavigate: (String) -> Unit, onTab: (String) -> Unit) {
    val vm: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val sessionVm: SessionViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity, factory = AppViewModelProvider.Factory
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val cart by sessionVm.cartCount.collectAsStateWithLifecycle()
    val role = user?.roleEnum ?: Role.CLIENTE

    val actions = when (role) {
        Role.CLIENTE -> listOf(
            QuickAction("Mapa", Icons.Filled.Map, Routes.MAP, Teal),
            QuickAction("Catálogo", Icons.Filled.LibraryBooks, Routes.CATALOG, Teal),
            QuickAction("Escáner QR", Icons.Filled.QrCodeScanner, Routes.QR_SCANNER, Teal),
            QuickAction("Favoritos", Icons.Filled.Favorite, Routes.FAVORITES, Gold),
            QuickAction("Solicitudes", Icons.AutoMirrored.Filled.ReceiptLong, Routes.ORDERS, Gold),
            QuickAction("Carrito${if (cart > 0) " ($cart)" else ""}", Icons.Filled.ShoppingCart, Routes.CART, Gold)
        )
        Role.ADMIN -> listOf(
            QuickAction("Solicitudes", Icons.AutoMirrored.Filled.ReceiptLong, Routes.ORDERS, Gold),
            QuickAction("Catálogo", Icons.Filled.Inventory2, Routes.CATALOG, Teal),
            QuickAction("Nuevo producto", Icons.Filled.AddBusiness, Routes.productForm(), Teal),
            QuickAction("Revisar museos", Icons.Filled.Map, Routes.MAP, Navy),
            QuickAction("Enviar aviso", Icons.Filled.Campaign, Routes.NOTIFICATIONS, Gold),
            QuickAction("Cuentas demo", Icons.Filled.Group, Routes.ADMIN_USERS, Navy)
        )
        Role.INVESTIGADOR -> listOf(
            QuickAction("Mapa", Icons.Filled.Map, Routes.MAP, Teal),
            QuickAction("Nueva ficha", Icons.Filled.NoteAdd, Routes.museumForm(), Navy),
            QuickAction("Escáner QR", Icons.Filled.QrCodeScanner, Routes.QR_SCANNER, Teal),
            QuickAction("Avisos", Icons.Filled.Notifications, Routes.NOTIFICATIONS, Gold)
        )
    }
    val tabRoutes = setOf(Routes.MAP, Routes.CATALOG, Routes.NOTIFICATIONS)
    val go: (String) -> Unit = { r -> if (r in tabRoutes) onTab(r) else onNavigate(r) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            // Encabezado
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Navy, NavyLight)))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("¡Hola, ${user?.name?.substringBefore(" ") ?: ""}!",
                            style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text(
                            when (role) {
                                Role.CLIENTE -> "Sigamos explorando el conocimiento del mundo."
                                Role.ADMIN -> "Panel de gestión de Museo de los Museos."
                                Role.INVESTIGADOR -> "Tus registros museológicos de Iberoamérica."
                            },
                            color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Tag(role.label, container = Gold, content = Color.White)
                    }
                    IconButton(onClick = { onTab(Routes.NOTIFICATIONS) }) {
                        // TODO (pendiente): mostrar contador de notificaciones no leídas
                        Icon(Icons.Filled.Notifications, "Notificaciones", tint = Color.White)
                    }
                    Box(Modifier.clickable { onTab(Routes.PROFILE) }) {
                        Avatar(user?.avatar ?: "avatar_1", size = 52.dp, name = user?.name ?: "")
                    }
                }
            }
            OfflineBanner(state.online)
        }

        item {
            // Accesos rápidos
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(actions) { a ->
                    Column(Modifier.width(76.dp).clickable { go(a.route) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(a.color), contentAlignment = Alignment.Center) {
                            Icon(a.icon, null, tint = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(a.label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        when (role) {
            Role.CLIENTE -> {
                item { SectionTitle("Explora") }
                item {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExploreCard("Cursos", "Aprende con expertos", Icons.Filled.School, Navy, Color.White, Modifier.weight(1f)) { onTab(Routes.CATALOG) }
                            ExploreCard("Productos", "Libros y más", Icons.Filled.Inventory2, GoldLight, Navy, Modifier.weight(1f)) { onTab(Routes.CATALOG) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExploreCard("Museos virtuales", "Recorre Iberoamérica", Icons.Filled.AccountBalance, TealLight, Navy, Modifier.weight(1f)) { onTab(Routes.MAP) }
                            ExploreCard("Novedades", "Exposiciones y eventos", Icons.Filled.Campaign, Color(0xFFF0ECE4), Navy, Modifier.weight(1f)) { onTab(Routes.NOTIFICATIONS) }
                        }
                    }
                }
            }
            Role.ADMIN -> {
                item {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Solicitudes pendientes", state.pendingOrders.size.toString(), Gold, Modifier.weight(1f)) { onNavigate(Routes.ORDERS) }
                        StatCard("Fichas por revisar", state.unpublishedMuseums.size.toString(), Teal, Modifier.weight(1f)) { onTab(Routes.MAP) }
                    }
                }
                item { SectionTitle("Novedades de gestión") }
                if (state.pendingOrders.isEmpty()) item {
                    Text("No hay solicitudes pendientes.", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(state.pendingOrders.take(5)) { o ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onNavigate(Routes.ORDERS) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${o.code} · ${o.userName}", style = MaterialTheme.typography.titleSmall)
                                Text(o.itemsSummary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(Format.money(o.total), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                state.unpublishedMuseums.take(3).forEach { m ->
                    item { MuseumRow(m, "Pendiente de publicación") { onNavigate(Routes.museum(m.id)) } }
                }
            }
            Role.INVESTIGADOR -> {
                item {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Museos registrados", state.totalMuseums.toString(), Navy, Modifier.weight(1f)) { onTab(Routes.MAP) }
                        StatCard("Pendientes / sin sincronizar", state.unpublishedMuseums.size.toString(), Gold, Modifier.weight(1f)) { onTab(Routes.MAP) }
                    }
                }
                item { SectionTitle("Registros museológicos por revisar") }
                if (state.unpublishedMuseums.isEmpty()) item {
                    Text("Todas las fichas están publicadas.", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(state.unpublishedMuseums) { m ->
                    MuseumRow(m, if (m.pendingSync) "Sin sincronizar" else "Pendiente de publicación") { onNavigate(Routes.museum(m.id)) }
                }
            }
        }

        item { SectionTitle("Museos destacados", action = { TextButton(onClick = { onTab(Routes.MAP) }) { Text("Ver todos") } }) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.featuredMuseums, key = { it.id }) { m -> FeaturedMuseum(m) { onNavigate(Routes.museum(m.id)) } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Sincronizar")
                }
            }
        }
    }
}

@Composable
private fun ExploreCard(title: String, subtitle: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(shown, modifier = modifier, enter = scaleIn(tween(400), initialScale = 0.85f) + fadeIn(tween(400))) {
        Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = bg), modifier = Modifier.height(110.dp)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, tint = fg)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun FeaturedMuseum(m: MuseumEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(220.dp)) {
        ReferenceImage(m.imageUrl, m.classification, Modifier.fillMaxWidth().height(120.dp))
        Column(Modifier.padding(12.dp)) {
            Text(m.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${m.city}, ${m.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Rating(m.rating)
        }
    }
}

@Composable
fun MuseumRow(m: MuseumEntity, badge: String? = null, trailing: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ReferenceImage(m.imageUrl, m.classification, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), iconSize = 28.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${m.city}, ${m.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (badge != null) { Spacer(Modifier.height(4.dp)); Tag(badge) }
            }
            trailing()
        }
    }
}
