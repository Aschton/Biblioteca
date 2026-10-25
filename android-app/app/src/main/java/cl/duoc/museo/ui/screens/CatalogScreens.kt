package cl.duoc.museo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.ProductEntity
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.isCourse
import cl.duoc.museo.ui.components.EmptyState
import cl.duoc.museo.ui.components.MuseoTopBar
import cl.duoc.museo.ui.components.OfflineBanner
import cl.duoc.museo.ui.components.Rating
import cl.duoc.museo.ui.components.ReferenceImage
import cl.duoc.museo.ui.components.Tag
import cl.duoc.museo.ui.components.rememberCameraCapture
import cl.duoc.museo.ui.components.toast
import cl.duoc.museo.ui.theme.Danger
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.theme.Success
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.CatalogViewModel
import cl.duoc.museo.ui.viewmodel.ProductDetailViewModel
import cl.duoc.museo.ui.viewmodel.ProductEvent
import cl.duoc.museo.ui.viewmodel.ProductFormViewModel
import cl.duoc.museo.ui.viewmodel.categories
import cl.duoc.museo.util.Format
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull

fun categoryLabel(c: String) = when (c) {
    "CURSO" -> "Curso"; "LIBRO" -> "Libro"; "MATERIAL" -> "Material de conservación"; else -> "Artículo museológico"
}

// =============== 9. Catálogo de Productos y Cursos ===============

@Composable
fun CatalogScreen(
    role: Role?,
    onOpen: (Long) -> Unit,
    onCart: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Long) -> Unit,
    cartCount: StateFlow<Int>
) {
    val vm: CatalogViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val cart by cartCount.collectAsStateWithLifecycle()
    val isAdmin = role == Role.ADMIN

    Scaffold(
        topBar = {
            MuseoTopBar(if (isAdmin) "Gestión de catálogo" else "Catálogo", actions = {
                IconButton(onClick = { vm.refresh() }) {
                    if (state.refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Refresh, "Sincronizar")
                }
                if (role == Role.CLIENTE) IconButton(onClick = onCart) {
                    BadgedBox(badge = { if (cart > 0) Badge { Text("$cart") } }) { Icon(Icons.Filled.ShoppingCart, "Carrito") }
                }
            })
        },
        floatingActionButton = {
            if (isAdmin) ExtendedFloatingActionButton(onClick = onNew, containerColor = Navy, contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nuevo") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OfflineBanner(state.online)
            OutlinedTextField(
                value = state.query, onValueChange = vm::setQuery,
                placeholder = { Text("Buscar productos, cursos…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true, shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { (label, value) ->
                    FilterChip(selected = state.category == value, onClick = { vm.setCategory(value) }, label = { Text(label) })
                }
                item { FilterChip(selected = state.onlyAvailable, onClick = vm::toggleAvailable, label = { Text("Disponibles") }) }
            }
            if (state.products.isEmpty()) {
                EmptyState(Icons.Filled.Inventory2, "No hay productos que coincidan con la búsqueda")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.products, key = { it.id }) { p ->
                        ProductCard(p, isAdmin, onClick = { onOpen(p.id) }, onEdit = { onEdit(p.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(p: ProductEntity, isAdmin: Boolean, onClick: () -> Unit, onEdit: () -> Unit) {
    Card(onClick = onClick) {
        Box {
            ReferenceImage(p.imageUrl, p.category, Modifier.fillMaxWidth().height(110.dp))
            if (!p.active) Box(Modifier.padding(6.dp)) { Tag("Inactivo", container = Color(0xFFFFE3D6)) }
        }
        Column(Modifier.padding(10.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(40.dp))
            Text(categoryLabel(p.category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Format.money(p.price), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (p.stock > 0) (if (p.isCourse) "${p.stock} cupos" else "Stock: ${p.stock}") else "Agotado",
                        style = MaterialTheme.typography.labelSmall, color = if (p.stock > 0) Success else Danger
                    )
                }
                if (isAdmin) FilledIconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, "Editar", Modifier.size(18.dp)) }
                else FilledIconButton(onClick = onClick, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.AddShoppingCart, "Ver", Modifier.size(18.dp)) }
            }
        }
    }
}

// =============== 10. Detalle y Solicitud de Compra ===============

@Composable
fun ProductDetailScreen(role: Role?, onBack: () -> Unit, onCart: () -> Unit, onEdit: (Long) -> Unit) {
    val vm: ProductDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val product by vm.product.collectAsStateWithLifecycle()
    val fav by vm.isFavorite.collectAsStateWithLifecycle()
    val qty by vm.qty.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var successBanner by remember { mutableStateOf<String?>(null) }
    val pop by animateFloatAsState(if (successBanner != null) 1.08f else 1f, label = "pop")

    LaunchedEffect(Unit) {
        vm.event.filterNotNull().collect { e ->
            vm.consumeEvent()
            when (e) {
                is ProductEvent.Added -> {
                    successBanner = "Agregado al carrito (${e.qty})"
                    val r = snackbar.showSnackbar("Producto agregado", actionLabel = "Ver carrito")
                    successBanner = null
                    if (r == SnackbarResult.ActionPerformed) onCart()
                }
                is ProductEvent.Error -> snackbar.showSnackbar(e.message)
                ProductEvent.InfoRequested -> {
                    successBanner = "Solicitud de información enviada"
                    snackbar.showSnackbar("Te contactaremos con más información del curso")
                    successBanner = null
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)) { padding ->
        val p = product
        if (p == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Box {
                ReferenceImage(p.imageUrl, p.category, Modifier.fillMaxWidth().height(260.dp), iconSize = 80.dp)
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
                    RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onBack)
                    Spacer(Modifier.weight(1f))
                    if (role == Role.CLIENTE) RoundIcon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorito",
                        { vm.toggleFavorite() }, tint = if (fav) Danger else Navy)
                }
            }
            Column(Modifier.padding(20.dp)) {
                Tag(categoryLabel(p.category))
                Spacer(Modifier.height(8.dp))
                Text(p.name, style = MaterialTheme.typography.headlineMedium)
                Text(Format.money(p.price), style = MaterialTheme.typography.headlineSmall, color = Gold)
                Rating(p.rating)
                Spacer(Modifier.height(12.dp))
                Text(p.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (p.isCourse) "Cupos" else "Cantidad", style = MaterialTheme.typography.titleSmall)
                        if (role == Role.CLIENTE && p.stock > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedIconButton(onClick = vm::dec, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Remove, "Menos") }
                            Text("$qty", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                            OutlinedIconButton(onClick = vm::inc, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Add, "Más") }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Disponibilidad", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (p.stock > 0 && p.active) Success else Danger))
                            Spacer(Modifier.width(6.dp))
                            Text(if (!p.active) "No disponible" else if (p.stock > 0) "En stock (${p.stock})" else "Agotado")
                        }
                    }
                }

                AnimatedVisibility(successBanner != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp).scale(pop).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFD9F2E3)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Success)
                        Spacer(Modifier.width(8.dp))
                        Text(successBanner ?: "", color = Success)
                    }
                }

                Spacer(Modifier.height(20.dp))
                when (role) {
                    Role.CLIENTE -> {
                        Button(
                            onClick = vm::addToCart, enabled = !busy && p.stock > 0 && p.active,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            else { Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.width(8.dp)); Text(if (p.isCourse) "Inscribirme (agregar al carrito)" else "Agregar al carrito") }
                        }
                        if (p.isCourse) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = vm::requestInfo, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                Icon(Icons.Filled.Info, null); Spacer(Modifier.width(8.dp)); Text("Solicitar información")
                            }
                        }
                        Text("La disponibilidad se valida con el servidor antes de confirmar.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    Role.ADMIN -> {
                        Button(onClick = { onEdit(p.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text("Modificar producto")
                        }
                        Spacer(Modifier.height(16.dp))
                        QrBlock(p.qrCode)
                    }
                    else -> QrBlock(p.qrCode)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// =============== Formulario de producto (Administrador: alta, modificación, desactivación) ===============

private val productCategories = listOf("CURSO", "LIBRO", "MATERIAL", "ARTICULO")

@Composable
fun ProductFormScreen(onBack: () -> Unit) {
    val vm: ProductFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val f by vm.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val takePhoto = rememberCameraCapture { path -> vm.update { it.copy(imageUrl = path) } }

    LaunchedEffect(f.saved) {
        if (f.saved) {
            toast(context, if (f.savedOnline) "Guardado en el servidor" else "Guardado localmente (sin conexión)")
            onBack()
        }
    }

    Scaffold(topBar = { MuseoTopBar(if (vm.isNew) "Nuevo producto o curso" else "Modificar producto", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp)) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp))) {
                ReferenceImage(f.imageUrl, f.category, Modifier.fillMaxSize())
                Button(onClick = takePhoto, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Foto")
                }
            }
            Spacer(Modifier.height(12.dp))
            Field(f.name, { v -> vm.update { it.copy(name = v) } }, "Nombre", f.errors["name"])
            DropdownField("Categoría", categoryLabel(f.category), productCategories.map(::categoryLabel)) { label ->
                vm.update { it.copy(category = productCategories.first { c -> categoryLabel(c) == label }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field(f.price, { v -> vm.update { it.copy(price = v) } }, "Precio (US$)", f.errors["price"], KeyboardType.Decimal) }
                Box(Modifier.weight(1f)) { Field(f.stock, { v -> vm.update { it.copy(stock = v) } }, if (f.category == "CURSO") "Cupos" else "Stock", f.errors["stock"], KeyboardType.Number) }
            }
            Field(f.description, { v -> vm.update { it.copy(description = v) } }, "Descripción", f.errors["description"], singleLine = false, minLines = 4)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Visible en el catálogo", Modifier.weight(1f))
                Switch(checked = f.active, onCheckedChange = { v -> vm.update { it.copy(active = v) } })
            }
            Text("Precios ficticios de prueba.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = vm::save, enabled = !f.saving, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                if (f.saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("Guardar")
            }
            if (!vm.isNew) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { vm.deactivate() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.Block, null, tint = Danger); Spacer(Modifier.width(8.dp)); Text("Desactivar producto", color = Danger)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
