package cl.duoc.museo.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.local.roleEnum
import cl.duoc.museo.ui.components.Avatar
import cl.duoc.museo.ui.components.MuseoTopBar
import cl.duoc.museo.ui.components.Tag
import cl.duoc.museo.ui.components.rememberCameraCapture
import cl.duoc.museo.ui.navigation.Routes
import cl.duoc.museo.ui.theme.Danger
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.viewmodel.AdminUsersViewModel
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.ProfileViewModel

private enum class ProfileDialog { DATA, INTERESTS, AVATAR, PASSWORD }

private val interests = listOf("Arte colonial", "Arte moderno", "Arqueología", "Conservación", "Curaduría", "Museología digital")

// =============== 15. Perfil y Configuración ===============

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onNavigate: (String) -> Unit) {
    val vm: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val user by vm.user.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<ProfileDialog?>(null) }
    var permTick by remember { mutableIntStateOf(0) }

    val takePhoto = rememberCameraCapture { path -> vm.save({ it.copy(avatar = path) }, "Avatar actualizado") }
    val requestPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permTick++ }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val u = user ?: return
    val role = u.roleEnum
    fun has(p: String) = permTick >= 0 && ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    Scaffold(topBar = { MuseoTopBar("Mi perfil") }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clickable { dialog = ProfileDialog.AVATAR }) { Avatar(u.avatar, 80.dp, u.name) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(u.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Tag(role.label)
                        Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Group("Cuenta") }
            item { ProfileItem(Icons.Filled.Person, "Mis datos personales", if (role == Role.CLIENTE) "Nombre, correo y dirección de envío" else "Nombre y correo") { dialog = ProfileDialog.DATA } }
            if (role == Role.CLIENTE) {
                item { ProfileItem(Icons.Filled.LocationOn, "Direcciones de envío", u.shippingAddress.ifBlank { "Sin dirección registrada" }) { dialog = ProfileDialog.DATA } }
                item { ProfileItem(Icons.Filled.Interests, "Mis intereses", u.interests.ifBlank { "Elige temas que te interesan" }) { dialog = ProfileDialog.INTERESTS } }
                item { ProfileItem(Icons.Filled.Favorite, "Mis favoritos", "Museos, recursos e historial QR") { onNavigate(Routes.FAVORITES) } }
            }
            if (role != Role.INVESTIGADOR) item {
                ProfileItem(Icons.AutoMirrored.Filled.ReceiptLong, if (role == Role.ADMIN) "Gestión de solicitudes" else "Mis solicitudes", "Historial y estados") { onNavigate(Routes.ORDERS) }
            }
            if (role == Role.ADMIN) item { ProfileItem(Icons.Filled.Group, "Cuentas de demostración", "Crear, activar o eliminar perfiles ficticios") { onNavigate(Routes.ADMIN_USERS) } }
            item { ProfileItem(Icons.Filled.Lock, "Cambiar contraseña", null) { dialog = ProfileDialog.PASSWORD } }

            item { Group("Permisos") }
            item {
                PermissionItem(Icons.Filled.PhotoCamera, "Cámara", has(Manifest.permission.CAMERA)) { requestPerm.launch(Manifest.permission.CAMERA) }
            }
            item {
                PermissionItem(Icons.Filled.LocationOn, "Ubicación", has(Manifest.permission.ACCESS_FINE_LOCATION)) { requestPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            }
            item {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Abrir ajustes de la app") }
            }

            item { Group("Sesión") }
            item {
                ProfileItem(Icons.Filled.SwitchAccount, "Cambiar de usuario", "Los datos de cada perfil se mantienen separados") {
                    onNavigate(Routes.LOGIN); vm.logout()
                }
            }
            item {
                OutlinedButton(
                    onClick = { vm.logout() },
                    border = BorderStroke(1.dp, Danger),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)
                ) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Danger); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión", color = Danger) }
            }
        }
    }

    when (dialog) {
        ProfileDialog.DATA -> {
            var name by remember { mutableStateOf(u.name) }
            var email by remember { mutableStateOf(u.email) }
            var address by remember { mutableStateOf(u.shippingAddress) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Mis datos personales") },
                text = {
                    Column {
                        Field(name, { name = it }, "Nombre completo")
                        Field(email, { email = it }, "Correo", keyboard = KeyboardType.Email)
                        if (role == Role.CLIENTE) Field(address, { address = it }, "Dirección de envío (ficticia)")
                    }
                },
                confirmButton = { TextButton(onClick = { if (vm.saveData(name, email, address)) dialog = null }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } }
            )
        }
        ProfileDialog.INTERESTS -> {
            var selected by remember { mutableStateOf(u.interests.split(",").filter { it.isNotBlank() }.toSet()) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Mis intereses") },
                text = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        interests.forEach { opt ->
                            val sel = opt in selected
                            FilterChip(sel, { selected = if (sel) selected - opt else selected + opt }, { Text(opt) })
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.save({ it.copy(interests = selected.joinToString(",")) }); dialog = null }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } }
            )
        }
        ProfileDialog.AVATAR -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Cambiar avatar") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (1..5).forEach { i ->
                            val key = "avatar_$i"
                            Box(Modifier.clickable { vm.save({ it.copy(avatar = key) }, "Avatar actualizado"); dialog = null }) {
                                Avatar(key, 44.dp, selected = u.avatar == key)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { dialog = null; takePhoto() }) {
                        Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Tomar foto con la cámara")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Cerrar") } }
        )
        ProfileDialog.PASSWORD -> {
            var current by remember { mutableStateOf("") }
            var new by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Cambiar contraseña") },
                text = {
                    Column {
                        Field(current, { current = it }, "Contraseña actual", isPassword = true, keyboard = KeyboardType.Password)
                        Field(new, { new = it }, "Nueva contraseña", isPassword = true, keyboard = KeyboardType.Password)
                    }
                },
                confirmButton = { TextButton(onClick = { vm.changePassword(current, new); dialog = null }) { Text("Actualizar") } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } }
            )
        }
        null -> {}
    }
}

@Composable
private fun Group(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun ProfileItem(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Navy)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: String, granted: Boolean, onRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Navy)
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (granted) Tag("Concedido", container = Color(0xFFD9F2E3), content = Color(0xFF1E6B3D))
        else TextButton(onClick = onRequest) { Text("Permitir") }
    }
}

// =============== Cuentas de demostración (Administrador) ===============

@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    val vm: AdminUsersViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val users by vm.users.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    Scaffold(
        topBar = { MuseoTopBar("Cuentas de demostración", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, containerColor = Navy, contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nueva cuenta") })
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Perfiles ficticios guardados en este teléfono. Nunca uses cuentas institucionales reales.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(users, key = { it.id }) { u ->
                Card {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.avatar, 44.dp, u.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.titleSmall)
                            Text(u.email, style = MaterialTheme.typography.bodySmall)
                            Tag(u.roleEnum.label)
                        }
                        val isSeedAdmin = u.email == "admin@museo.cl"
                        Switch(checked = u.active, onCheckedChange = { vm.toggle(u) }, enabled = !isSeedAdmin)
                        IconButton(onClick = { vm.delete(u) }, enabled = !isSeedAdmin) { Icon(Icons.Filled.Delete, "Eliminar") }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("Demo1234") }
        var role by remember { mutableStateOf(Role.CLIENTE) }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("Nueva cuenta ficticia") },
            text = {
                Column {
                    Field(name, { name = it }, "Nombre")
                    Field(email, { email = it }, "Correo", keyboard = KeyboardType.Email)
                    Field(password, { password = it }, "Contraseña")
                    DropdownField("Rol", role.label, Role.entries.map { it.label }) { l -> role = Role.entries.first { it.label == l } }
                }
            },
            confirmButton = { TextButton(onClick = { vm.create(name, email, password, role); creating = false }) { Text("Crear") } },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancelar") } }
        )
    }
}
