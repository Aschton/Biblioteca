package cl.duoc.museo.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.Role
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
import cl.duoc.museo.ui.theme.TealLight
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.MapFilter
import cl.duoc.museo.ui.viewmodel.MapViewModel
import cl.duoc.museo.ui.viewmodel.MuseumDetailViewModel
import cl.duoc.museo.ui.viewmodel.MuseumFormViewModel
import cl.duoc.museo.util.QrGenerator
import cl.duoc.museo.util.ShippingCalculator
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

// =============== 5. Mapa de Museos Virtuales ===============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onOpenMuseum: (Long) -> Unit, role: Role?, onNewMuseum: () -> Unit) {
    val vm: MapViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locationOverlay?.let { it.enableMyLocation(); it.enableFollowLocation() }
        else toast(context, "Permiso de ubicación denegado")
    }

    // Ciclo de vida del MapView de osmdroid
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Al filtrar, centrar el mapa en los museos resultantes
    LaunchedEffect(state.filterValue, state.museums.size) {
        val map = mapView ?: return@LaunchedEffect
        val list = state.museums
        if (state.filterValue != null && list.isNotEmpty()) {
            map.controller.animateTo(GeoPoint(list.map { it.latitude }.average(), list.map { it.longitude }.average()))
            map.controller.setZoom(if (list.size == 1) 11.0 else 5.0)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    minZoomLevel = 2.5
                    controller.setZoom(3.2)
                    controller.setCenter(GeoPoint(5.0, -45.0)) // Iberoamérica
                    val overlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    overlays.add(overlay)
                    locationOverlay = overlay
                    mapView = this
                }
            },
            update = { map ->
                map.overlays.removeAll { it is Marker }
                state.museums.forEach { m ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(m.latitude, m.longitude)
                        title = m.name
                        snippet = "${m.city}, ${m.country}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ -> vm.select(m); true }
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            },
            onRelease = { it.onDetach() }
        )

        // Filtros
        Column(Modifier.statusBarsPadding().padding(12.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 4.dp) {
                Column(Modifier.padding(8.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        MapFilter.entries.forEachIndexed { i, f ->
                            SegmentedButton(
                                selected = state.filter == f,
                                onClick = { vm.setFilter(f) },
                                shape = SegmentedButtonDefaults.itemShape(i, MapFilter.entries.size)
                            ) { Text(when (f) { MapFilter.TODOS -> "Todos"; MapFilter.PAIS -> "País"; MapFilter.PROYECTO -> "Proyecto" }) }
                        }
                    }
                    if (state.options.isNotEmpty()) {
                        LazyRow(contentPadding = PaddingValues(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.options) { opt ->
                                FilterChip(
                                    selected = state.filterValue == opt,
                                    onClick = { vm.setFilterValue(if (state.filterValue == opt) null else opt) },
                                    label = { Text(opt) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            OfflineBanner(state.online)
            Text(
                "${state.museums.size} museos en el mapa",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
            FloatingActionButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationOverlay?.let { it.enableMyLocation(); it.enableFollowLocation() }
                    mapView?.controller?.setZoom(10.0)
                } else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }, containerColor = Color.White) { Icon(Icons.Filled.MyLocation, "Mi ubicación", tint = Navy) }
            if (role == Role.INVESTIGADOR) {
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(onClick = onNewMuseum, containerColor = Navy, contentColor = Color.White,
                    icon = { Icon(Icons.Filled.NoteAdd, null) }, text = { Text("Nueva ficha") })
            }
            Spacer(Modifier.height(if (state.selected != null) 170.dp else 0.dp))
        }

        // Tarjeta del museo seleccionado
        AnimatedVisibility(
            visible = state.selected != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it }, exit = slideOutVertically { it }
        ) {
            state.selected?.let { m ->
                Card(modifier = Modifier.fillMaxWidth().padding(12.dp), onClick = { onOpenMuseum(m.id) }) {
                    Row(Modifier.padding(10.dp)) {
                        ReferenceImage(m.imageUrl, m.classification, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Danger)
                                Text("${m.city}, ${m.country}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag(m.classification)
                                Tag("Virtual", container = TealLight)
                                if (!m.published) Tag("Pendiente", container = Color(0xFFFFE3D6))
                            }
                            Spacer(Modifier.height(4.dp))
                            Rating(m.rating)
                        }
                        IconButton(onClick = { vm.select(null) }) { Icon(Icons.Filled.Close, "Cerrar") }
                    }
                }
            }
        }
    }
}

// =============== 6. Detalle del Museo Virtual ===============

@Composable
fun MuseumDetailScreen(role: Role?, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val vm: MuseumDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val museum by vm.museum.collectAsStateWithLifecycle()
    val favorite by vm.isFavorite.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)) { padding ->
        val m = museum
        if (m == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Box {
                ReferenceImage(m.imageUrl, m.classification, Modifier.fillMaxWidth().height(260.dp), iconSize = 80.dp)
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
                    RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onBack)
                    Spacer(Modifier.weight(1f))
                    if (role == Role.CLIENTE) RoundIcon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorito", vm::toggleFavorite,
                        tint = if (favorite) Danger else Navy
                    )
                }
            }
            Column(Modifier.padding(20.dp)) {
                Text(m.name, style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp), tint = Danger)
                    Text(" ${m.city}, ${m.country}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(m.classification)
                    Tag(m.project, container = MaterialTheme.colorScheme.primaryContainer, content = Navy)
                    Tag("Virtual", container = TealLight)
                }
                Spacer(Modifier.height(8.dp))
                Rating(m.rating)
                if (!m.published || m.pendingSync) {
                    Spacer(Modifier.height(8.dp))
                    Tag(if (m.pendingSync) "Guardado sin sincronizar" else "Pendiente de publicación", container = Color(0xFFFFE3D6))
                }
                Spacer(Modifier.height(16.dp))
                Text(m.description, style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(20.dp))
                // Se diferencia la ubicación física del enlace a la visita virtual
                InfoBlock(Icons.Filled.LocationOn, "Ubicación geográfica",
                    "${m.city}, ${m.country}\nLat ${"%.4f".format(Locale.US, m.latitude)} · Lng ${"%.4f".format(Locale.US, m.longitude)}")
                InfoBlock(Icons.Filled.Public, "Visita virtual", m.virtualTourUrl)

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.virtualTourUrl))) }
                            .onFailure { toast(context, "No hay navegador disponible") }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Icon(Icons.AutoMirrored.Filled.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Ver proyecto virtual") }

                if (role == Role.CLIENTE) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = vm::toggleFavorite, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (favorite) Danger else Navy)
                        Spacer(Modifier.width(8.dp))
                        Text(if (favorite) "Quitar de favoritos" else "Guardar en favoritos")
                    }
                }

                if (role == Role.INVESTIGADOR) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onEdit(m.id) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text("Corregir ficha museológica")
                    }
                }

                if (role == Role.ADMIN) {
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Publicación", style = MaterialTheme.typography.titleSmall)
                                Text(if (m.published) "Visible para clientes" else "Oculta, en revisión", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = m.published, onCheckedChange = { vm.setPublished(it) })
                        }
                    }
                }

                if (role == Role.INVESTIGADOR || role == Role.ADMIN) {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    QrBlock(m.qrCode)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun QrBlock(code: String) {
    val bitmap = remember(code) { QrGenerator.bitmap(code, 400) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Código QR vinculado", style = MaterialTheme.typography.titleSmall)
        Text("Escanéalo desde otro teléfono o guarda una captura y usa \"Desde galería\"",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Image(bitmap.asImageBitmap(), "QR $code", Modifier.size(180.dp))
        Text(code, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun InfoBlock(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Navy)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit, tint: Color = Navy) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.92f), shadowElevation = 2.dp) {
        IconButton(onClick = onClick) { Icon(icon, desc, tint = tint) }
    }
}

// =============== Formulario de ficha museológica (Investigador) ===============

private val classifications = listOf("Historia", "Bellas artes", "Arte moderno", "Arte contemporáneo", "Arqueología", "Ciencias")
private val projects = listOf("Red Iberoamericana", "Patrimonio Colonial Digital", "Arte Moderno en Línea")

@Composable
fun MuseumFormScreen(onBack: () -> Unit) {
    val vm: MuseumFormViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val f by vm.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val takePhoto = rememberCameraCapture { path -> vm.update { it.copy(imageUrl = path) } }

    LaunchedEffect(f.saved) {
        if (f.saved) {
            toast(context, if (f.savedOnline) "Ficha guardada en el servidor" else "Ficha guardada localmente (se sincronizará luego)")
            onBack()
        }
    }

    Scaffold(topBar = { MuseoTopBar(if (vm.isNew) "Registrar museo virtual" else "Corregir ficha", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp)) {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))) {
                ReferenceImage(f.imageUrl, f.classification, Modifier.fillMaxSize())
                Button(onClick = takePhoto, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(6.dp))
                    Text(if (f.imageUrl.isBlank()) "Tomar foto" else "Cambiar foto")
                }
            }
            Spacer(Modifier.height(12.dp))
            Field(f.name, { v -> vm.update { it.copy(name = v) } }, "Nombre del museo", f.errors["name"])
            DropdownField("País", f.country, ShippingCalculator.countries.filter { it != "Otro" }) { v -> vm.update { it.copy(country = v) } }
            Field(f.city, { v -> vm.update { it.copy(city = v) } }, "Ciudad", f.errors["city"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    Field(f.latitude, { v -> vm.update { it.copy(latitude = v) } }, "Latitud", f.errors["latitude"], KeyboardType.Decimal)
                }
                Box(Modifier.weight(1f)) {
                    Field(f.longitude, { v -> vm.update { it.copy(longitude = v) } }, "Longitud", f.errors["longitude"], KeyboardType.Decimal)
                }
            }
            DropdownField("Clasificación", f.classification, classifications) { v -> vm.update { it.copy(classification = v) } }
            DropdownField("Proyecto", f.project, projects) { v -> vm.update { it.copy(project = v) } }
            Field(f.virtualTourUrl, { v -> vm.update { it.copy(virtualTourUrl = v) } }, "Enlace a la visita virtual", f.errors["virtualTourUrl"], KeyboardType.Uri)
            Field(f.description, { v -> vm.update { it.copy(description = v) } }, "Descripción", f.errors["description"], singleLine = false, minLines = 4)
            Spacer(Modifier.height(8.dp))
            Text("Las fichas nuevas quedan pendientes hasta que el Administrador revise su publicación.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.save(publishDirectly = false) }, enabled = !f.saving,
                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                if (f.saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Guardar ficha")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.padding(vertical = 3.dp)) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            keyboardOptions = KeyboardOptions.Default
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
