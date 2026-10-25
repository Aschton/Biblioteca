package cl.duoc.museo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.ui.components.Avatar
import cl.duoc.museo.ui.components.MuseoTopBar
import cl.duoc.museo.ui.components.rememberCameraCapture
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.theme.GoldLight
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.theme.NavyLight
import cl.duoc.museo.ui.theme.Teal
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.AuthViewModel

// =============== 1. Bienvenida / Inicio ===============

@Composable
fun WelcomeScreen(onStart: () -> Unit, onRegister: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyLight, Color(0xFF3A2E1F))))
            .safeDrawingPadding()
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(900)) + slideInVertically(tween(900)) { it / 6 }
        ) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.AccountBalance, null, tint = Gold, modifier = Modifier.size(84.dp).scale(scale))
                Spacer(Modifier.height(20.dp))
                Text("Museo de los Museos", style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Conecta, aprende y explora el patrimonio de Iberoamérica",
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Educación museológica, investigación aplicada, formación profesional y desarrollo de audiencias.",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Feature(Icons.AutoMirrored.Filled.MenuBook, "Catálogo")
                    Feature(Icons.Filled.Map, "Mapa virtual")
                    Feature(Icons.Filled.School, "Cursos")
                }
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Text("Comenzar", style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Crear cuenta de cliente") }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, label: String) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = GoldLight)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

// =============== 3. Selección de rol / Inicio de sesión ===============

private data class RoleOption(val role: Role, val subtitle: String, val icon: ImageVector, val color: Color, val demoEmail: String, val demoPass: String)

private val roleOptions = listOf(
    RoleOption(Role.ADMIN, "Gestión completa del sistema", Icons.Filled.WorkspacePremium, Gold, "admin@museo.cl", "Admin123"),
    RoleOption(Role.INVESTIGADOR, "Registro de museos y proyectos", Icons.Outlined.Biotech, Navy, "investigador@museo.cl", "Investiga123"),
    RoleOption(Role.CLIENTE, "Explora, aprende y compra", Icons.Filled.Person, Teal, "cliente@museo.cl", "Cliente123")
)

@Composable
fun LoginScreen(onBack: () -> Unit, onLoggedIn: () -> Unit, onRegister: () -> Unit) {
    val vm: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    var role by rememberSaveable { mutableStateOf(Role.CLIENTE) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoggedIn() }

    Scaffold(topBar = { MuseoTopBar("", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.AccountBalance, null, tint = Navy, modifier = Modifier.size(44.dp))
            Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
            Text("Selecciona tu tipo de cuenta", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            roleOptions.forEach { opt ->
                val selected = role == opt.role
                Card(
                    onClick = { role = opt.role; vm.clearError() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected) GoldLight else MaterialTheme.colorScheme.surface),
                    border = if (selected) BorderStroke(2.dp, Gold) else null
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(opt.color), contentAlignment = Alignment.Center) {
                            Icon(opt.icon, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(opt.role.label, style = MaterialTheme.typography.titleMedium)
                            Text(opt.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Contraseña") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar contraseña")
                    }
                }
            )
            state.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { showHelp = true }) { Text("¿Olvidaste tu contraseña?") }

            Button(
                onClick = { vm.login(email, password, role) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Iniciar sesión")
            }

            TextButton(onClick = {
                val opt = roleOptions.first { it.role == role }
                email = opt.demoEmail; password = opt.demoPass
            }) { Text("Usar credenciales de prueba (${role.label})") }

            TextButton(onClick = onRegister) { Text("¿No tienes cuenta? Regístrate como cliente") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido") } },
        title = { Text("Credenciales académicas de prueba") },
        text = {
            Text(roleOptions.joinToString("\n\n") { "${it.role.label}\n${it.demoEmail} / ${it.demoPass}" } +
                "\n\nSon cuentas ficticias del MVP. Nunca uses cuentas institucionales reales.")
        }
    )
}

// =============== 2. Registro de usuario (Cliente) ===============

private val interestOptions = listOf("Arte colonial", "Arte moderno", "Arqueología", "Conservación", "Curaduría", "Museología digital")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(onBack: () -> Unit, onRegistered: () -> Unit, onLogin: () -> Unit) {
    val vm: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    var showPass by remember { mutableStateOf(false) }
    val takePhoto = rememberCameraCapture { path -> vm.updateForm { it.copy(avatar = path) } }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onRegistered() }

    Scaffold(topBar = { MuseoTopBar("Registro de usuario", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Crear tu cuenta", style = MaterialTheme.typography.headlineMedium)
            Text("Únete a la comunidad de Museo de los Museos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Avatar(form.avatar, size = 96.dp, name = form.name)
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Navy).clickable { takePhoto() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.CameraAlt, "Tomar foto", tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(16.dp))

            Field(form.name, { v -> vm.updateForm { it.copy(name = v, nameError = null) } }, "Nombre completo", form.nameError)
            Field(form.email, { v -> vm.updateForm { it.copy(email = v, emailError = null) } }, "Correo electrónico", form.emailError, KeyboardType.Email)
            Field(form.password, { v -> vm.updateForm { it.copy(password = v, passwordError = null) } }, "Contraseña", form.passwordError,
                KeyboardType.Password, isPassword = !showPass,
                trailing = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar")
                    }
                })
            Field(form.confirm, { v -> vm.updateForm { it.copy(confirm = v, confirmError = null) } }, "Confirmar contraseña", form.confirmError,
                KeyboardType.Password, isPassword = !showPass)

            Text("Selecciona un avatar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (1..5).forEach { i ->
                    val key = "avatar_$i"
                    Box(Modifier.clickable { vm.updateForm { it.copy(avatar = key) } }) {
                        Avatar(key, size = 52.dp, selected = form.avatar == key)
                    }
                }
            }

            Text("Tus intereses (opcional)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                interestOptions.forEach { opt ->
                    val sel = opt in form.interests
                    FilterChip(selected = sel, onClick = {
                        vm.updateForm { it.copy(interests = if (sel) it.interests - opt else it.interests + opt) }
                    }, label = { Text(opt) })
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = vm::register, enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Registrarme")
            }
            TextButton(onClick = onLogin) { Text("¿Ya tienes cuenta? Inicia sesión", fontWeight = FontWeight.Medium) }
            Text(
                "Tu perfil, favoritos, historial y solicitudes se guardan en este teléfono (Room/SQLite) separados de otros usuarios.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        singleLine = singleLine, minLines = minLines,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}
