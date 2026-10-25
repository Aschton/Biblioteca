package cl.duoc.museo.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.OrdersViewModel
import cl.duoc.museo.util.Format

// =============== 13. Historial y Gestión de Solicitudes ===============

private val statusFilters = listOf("Todas" to null) + OrderStatus.all.map { OrderStatus.label(it) + "s" to it }

@Composable
fun OrdersScreen(onBack: () -> Unit) {
    val vm: OrdersViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<OrderEntity?>(null) }

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
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.updateStatus(order, selected); editing = null }, enabled = selected != order.status) { Text("Actualizar") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancelar") } }
        )
    }
}

// =============== 14. Notificaciones (EN DESARROLLO) ===============
// TODO (pendiente): listar avisos, filtros por tipo y notificaciones push.

@Composable
fun NotificationsScreen(role: Role?) {
    Scaffold(topBar = { MuseoTopBar("Notificaciones") }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize())
    }
}
