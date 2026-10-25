package cl.duoc.museo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.OrderEntity
import cl.duoc.museo.data.local.isCourse
import cl.duoc.museo.ui.components.EmptyState
import cl.duoc.museo.ui.components.MuseoTopBar
import cl.duoc.museo.ui.components.ReferenceImage
import cl.duoc.museo.ui.components.SuccessCheck
import cl.duoc.museo.ui.components.Tag
import cl.duoc.museo.ui.theme.Danger
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.CartEvent
import cl.duoc.museo.ui.viewmodel.CartViewModel
import cl.duoc.museo.ui.viewmodel.FavoritesViewModel
import cl.duoc.museo.util.Format
import cl.duoc.museo.util.ShippingCalculator
import cl.duoc.museo.util.ShippingCalculator.ShippingType
import kotlinx.coroutines.flow.filterNotNull

// =============== 11. Colección de Museos Favoritos ===============

@Composable
fun FavoritesScreen(onBack: () -> Unit, onMuseum: (Long) -> Unit, onProduct: (Long) -> Unit) {
    val vm: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val tabs = listOf("Museos (${s.museums.size})", "Recursos (${s.products.size})", "Consultados", "Escaneados")

    Scaffold(topBar = { MuseoTopBar("Mis favoritos", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                when (tab) {
                    0 -> {
                        if (s.museums.isEmpty()) item { EmptyState(Icons.Filled.FavoriteBorder, "Aún no guardas museos. Toca el corazón en la ficha de un museo.") }
                        items(s.museums, key = { it.id }) { m ->
                            MuseumRow(m, trailing = {
                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(onClick = { vm.removeMuseum(m.id) }) { Icon(Icons.Filled.Favorite, "Quitar", tint = Danger) }
                                    IconButton(onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.virtualTourUrl))) }
                                    }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Visita virtual") }
                                }
                            }) { onMuseum(m.id) }
                        }
                    }
                    1 -> {
                        if (s.products.isEmpty()) item { EmptyState(Icons.Filled.FavoriteBorder, "No tienes recursos guardados.") }
                        items(s.products, key = { it.id }) { p ->
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onProduct(p.id) }) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ReferenceImage(p.imageUrl, p.category, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), iconSize = 28.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, style = MaterialTheme.typography.titleSmall)
                                        Text(Format.money(p.price), style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { vm.removeProduct(p.id) }) { Icon(Icons.Filled.Favorite, "Quitar", tint = Danger) }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (s.viewed.isEmpty()) item { EmptyState(Icons.Filled.History, "Los museos que consultes aparecerán aquí.") }
                        items(s.viewed, key = { it.id }) { m -> MuseumRow(m) { onMuseum(m.id) } }
                    }
                    else -> {
                        if (s.qrHistory.isEmpty()) item { EmptyState(Icons.Filled.QrCode2, "Tu historial de códigos QR está vacío.") }
                        items(s.qrHistory, key = { it.id }) { h ->
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                onClick = { if (h.itemType == "MUSEUM") onMuseum(h.itemId) else onProduct(h.itemId) }) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.QrCode2, null)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(h.title, style = MaterialTheme.typography.titleSmall)
                                        Text("${h.code} · ${Format.date(h.scannedAt)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============== 12. Carrito y Cálculo de Envío ===============

@Composable
fun CartScreen(onBack: () -> Unit, onOrders: () -> Unit, onCatalog: () -> Unit) {
    val vm: CartViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val s by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirm by remember { mutableStateOf(false) }
    var placed by remember { mutableStateOf<OrderEntity?>(null) }

    LaunchedEffect(Unit) {
        vm.event.filterNotNull().collect { e ->
            vm.consumeEvent()
            when (e) {
                is CartEvent.Placed -> placed = e.order
                is CartEvent.Error -> snackbar.showSnackbar(e.message)
            }
        }
    }

    Scaffold(topBar = { MuseoTopBar("Mi carrito", onBack = onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (s.lines.isEmpty() && placed == null) {
            Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(Icons.Filled.ShoppingCart, "Tu carrito está vacío")
                Button(onClick = onCatalog) { Text("Ir al catálogo") }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.lines, key = { it.product.id }) { line ->
                Card {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ReferenceImage(line.product.imageUrl, line.product.category, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), iconSize = 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(line.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(Format.money(line.product.price) + if (line.product.isCourse) " · digital" else "", style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedIconButton(onClick = { vm.setQty(line.product.id, line.quantity - 1) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Remove, "Menos", Modifier.size(16.dp)) }
                                Text("${line.quantity}", modifier = Modifier.padding(horizontal = 12.dp))
                                OutlinedIconButton(
                                    onClick = { vm.setQty(line.product.id, (line.quantity + 1).coerceAtMost(line.product.stock)) },
                                    modifier = Modifier.size(30.dp)
                                ) { Icon(Icons.Filled.Add, "Más", Modifier.size(16.dp)) }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = { vm.setQty(line.product.id, 0) }) { Icon(Icons.Filled.Delete, "Eliminar") }
                            Text(Format.money(line.product.price * line.quantity), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalShipping, null); Spacer(Modifier.width(8.dp))
                            Text("Calcular envío", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        DropdownField("País de destino", s.country, ShippingCalculator.countries) { vm.setCountry(it) }
                        Text(if (ShippingCalculator.isNational(s.country)) "Envío nacional" else "Envío internacional",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DropdownField(
                            "Tipo de envío",
                            "${s.type.label} (${s.type.days})",
                            ShippingType.entries.map { "${it.label} (${it.days})" }
                        ) { label -> vm.setType(ShippingType.entries.first { "${it.label} (${it.days})" == label }) }
                        s.shippingError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        if (s.onlyDigital) Text("Solo cursos digitales: no se cobra envío.", style = MaterialTheme.typography.bodySmall)
                        Text("Tarifas simuladas con reglas ficticias, sin conexión a empresas de transporte reales.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Column(Modifier.padding(vertical = 8.dp)) {
                    SummaryRow("Subtotal", Format.money2(s.subtotal))
                    SummaryRow("Envío", Format.money2(s.shipping))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    SummaryRow("Total", Format.money2(s.total), bold = true)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { confirm = true }, enabled = !busy && s.shippingError == null,
                        modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Filled.Lock, null); Spacer(Modifier.width(8.dp)); Text("Proceder a la solicitud") }
                    }
                }
            }
        }
    }

    // Confirmación con el total final antes de enviar
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Confirmar solicitud") },
        text = {
            Column {
                s.lines.forEach { Text("${it.quantity} × ${it.product.name}", style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(8.dp))
                Text("Destino: ${s.country} · ${s.type.label}")
                Text("Subtotal: ${Format.money2(s.subtotal)}")
                Text("Envío: ${Format.money2(s.shipping)}")
                Text("Total final: ${Format.money2(s.total)}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Pago simulado: no se realizará ningún cobro real.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { confirm = false; vm.checkout() }) { Text("Enviar solicitud") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } }
    )

    placed?.let { order ->
        AlertDialog(
            onDismissRequest = { placed = null; onOrders() },
            icon = { SuccessCheck(64.dp) },
            title = { Text("¡Solicitud enviada!") },
            text = {
                Text(
                    "Tu pedido ${order.code} quedó en estado Pendiente.\nTotal: ${Format.money2(order.total)}" +
                        if (!order.synced) "\n\n(Guardado sin conexión; se enviará al servidor al sincronizar.)" else "",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = { TextButton(onClick = { placed = null; onOrders() }) { Text("Ver mis solicitudes") } }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, Modifier.weight(1f), style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (bold) Gold else MaterialTheme.colorScheme.onSurface)
    }
}

/** Reutilizado por pantallas de historial. */
@Composable
fun StatusTag(status: String) {
    val (bg, fg) = cl.duoc.museo.ui.components.statusColors(status)
    Tag(status.lowercase().replaceFirstChar { it.uppercase() }, container = bg, content = fg)
}
