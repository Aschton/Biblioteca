package cl.duoc.museo.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.OrderStatus
import cl.duoc.museo.ui.components.EmptyState
import cl.duoc.museo.ui.components.MuseoTopBar
import cl.duoc.museo.ui.components.OfflineBanner
import cl.duoc.museo.ui.components.Tag
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.theme.Teal
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.NotificationsViewModel
import cl.duoc.museo.ui.viewmodel.OrdersViewModel
import cl.duoc.museo.util.Format

// =============== 13. Historial y Gestión de Solicitudes ===============

private val statusFilters = listOf("Todas" to null) + OrderStatus.all.map { OrderStatus.label(it) + "s" to it }

@Composable
fun OrdersScreen(onBack: () -> Unit) {
    val vm: OrdersViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val s by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<OrderEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(message) { message?.let { cl.duoc.museo.ui.components.toast(context, it); vm.clearMessage() } }

    Scaffold(topBar = {
        MuseoTopBar(if (s.isAdmin) "Gestión de solicitudes" else "Mis solicitudes", onBack = onBack, actions = {
            IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, "Actualizar") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OfflineBanner(s.online)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statusFilters) { (label, value) ->
                    FilterChip(selected = s.filter == value, onClick = { vm.setFilter(value) }, label = { Text(label) })
                }
            }
            if (s.orders.isEmpty()) EmptyState(Icons.AutoMirrored.Filled.ReceiptLong, "No hay solicitudes para mostrar")
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.orders, key = { it.id }) { o ->
                    Card(
                        Modifier.fillMaxWidth().animateContentSize().clickable(enabled = s.isAdmin) { editing = o }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.code, style = MaterialTheme.typography.titleSmall)
                                Text(Format.date(o.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (s.isAdmin) Text(o.userName, style = MaterialTheme.typography.bodySmall)
                                Text(o.itemsSummary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (o.kind == "INFO_CURSO") "Solicitud de información" else "${o.itemsCount} producto(s) · ${Format.money(o.total)}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (!o.synced) Text("Sin sincronizar", style = MaterialTheme.typography.labelSmall, color = Gold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Crossfade(o.status, label = "status") { st -> StatusTag(st) }
                                if (s.isAdmin) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Cambiar estado")
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { order ->
        var selected by remember(order.id) { mutableStateOf(order.status) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Estado de ${order.code}") },
            text = {
                Column {
                    Text("${order.userName} · ${order.userEmail}", style = MaterialTheme.typography.bodySmall)
                    Text("Destino: ${order.country} · ${order.shippingType}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OrderStatus.all.forEach { st ->
                        Row(Modifier.fillMaxWidth().clickable { selected = st }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == st, onClick = { selected = st })
                            Text(OrderStatus.label(st))
                        }
                    }
                    Text("El cliente recibirá una notificación del cambio.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.updateStatus(order, selected); editing = null }, enabled = selected != order.status) { Text("Actualizar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.delete(order); editing = null }) {
                        Text("Eliminar", color = cl.duoc.museo.ui.theme.Danger)
                    }
                    TextButton(onClick = { editing = null }) { Text("Cancelar") }
                }
            }
        )
    }
}

// =============== 14. Notificaciones ===============

private val notifFilters = listOf("Todas" to null, "Cursos" to "CURSO", "Museos" to "MUSEO", "Pedidos" to "PEDIDO", "Publicaciones" to "PUBLICACION", "Eventos" to "EVENTO")

private fun notifStyle(category: String): Pair<ImageVector, Color> = when (category) {
    "CURSO" -> Icons.Filled.School to Color(0xFF7B8794)
    "MUSEO" -> Icons.Filled.AccountBalance to Gold
    "PEDIDO" -> Icons.Filled.LocalShipping to Color(0xFF6C7BD9)
    "PUBLICACION" -> Icons.Filled.AutoStories to Teal
    "EVENTO" -> Icons.Filled.Event to Color(0xFFD9534F)
    else -> Icons.Filled.Notifications to Navy
}

@Composable
fun NotificationsScreen(role: Role?, onOpen: (String) -> Unit) {
    val vm: NotificationsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val s by vm.state.collectAsStateWithLifecycle()
    val sent by vm.sent.collectAsStateWithLifecycle()
    var compose by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(sent) { if (sent) { vm.consumeSent(); snackbar.showSnackbar("Aviso enviado como notificación push") } }

    Scaffold(
        topBar = {
            MuseoTopBar("Notificaciones", actions = {
                IconButton(onClick = { vm.markAllRead() }) { Icon(Icons.Filled.DoneAll, "Marcar todo como leído") }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (role == Role.ADMIN) ExtendedFloatingActionButton(
                onClick = { compose = true }, containerColor = Navy, contentColor = Color.White,
                icon = { Icon(Icons.Filled.Campaign, null) }, text = { Text("Aviso de prueba") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifFilters) { (label, value) ->
                    FilterChip(selected = s.filter == value, onClick = { vm.setFilter(value) }, label = { Text(label) })
                }
            }
            if (s.items.isEmpty()) EmptyState(Icons.Filled.Notifications, "No tienes notificaciones")
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(s.items, key = { it.notification.id }) { item ->
                    val n = item.notification
                    val (icon, color) = notifStyle(n.category)
                    Card(
                        onClick = { vm.markRead(n.id); onOpen(n.destination) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(n.title, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold)
                                Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Format.relative(n.createdAt), style = MaterialTheme.typography.labelSmall)
                                if (!item.isRead) {
                                    Spacer(Modifier.height(6.dp))
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Gold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (compose) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("EVENTO") }
        var audience by remember { mutableStateOf("Todos") }
        val audiences = mapOf("Todos" to "ALL", "Clientes" to Role.CLIENTE.name, "Investigadores" to Role.INVESTIGADOR.name)
        val catLabels: Map<String, String> = notifFilters.drop(1).associate { it.first to it.second!! }
        AlertDialog(
            onDismissRequest = { compose = false },
            title = { Text("Nuevo aviso de prueba") },
            text = {
                Column {
                    Field(title, { title = it }, "Título")
                    Field(body, { body = it }, "Mensaje", singleLine = false, minLines = 2)
                    DropdownField("Categoría", catLabels.entries.first { it.value == category }.key, catLabels.keys.toList()) { category = catLabels.getValue(it) }
                    DropdownField("Destinatarios", audience, audiences.keys.toList()) { audience = it }
                    Tag("Se mostrará como notificación push local")
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && body.isNotBlank(), onClick = {
                    vm.sendTest(title.trim(), body.trim(), category, audiences.getValue(audience))
                    compose = false
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { compose = false }) { Text("Cancelar") } }
        )
    }
}
