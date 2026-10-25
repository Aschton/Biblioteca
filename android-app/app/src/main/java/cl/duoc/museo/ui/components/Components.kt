package cl.duoc.museo.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.theme.NavyLight
import cl.duoc.museo.ui.theme.Teal
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuseoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/** Paleta de color según categoría, para imágenes referenciales sin foto. */
private fun paletteFor(key: String): Pair<Color, Color> = when (key.lowercase()) {
    "historia", "arqueología" -> Color(0xFF6B4E2E) to Color(0xFFB8945A)
    "arte moderno", "arte contemporáneo" -> Color(0xFF1D4269) to Color(0xFF2A8C82)
    "bellas artes" -> Color(0xFF5B2333) to Color(0xFFB8945A)
    "curso" -> Color(0xFF0E2A47) to Color(0xFF2A8C82)
    "libro" -> Color(0xFF6B2E2E) to Color(0xFFC8945A)
    "material" -> Color(0xFF3E5B4A) to Color(0xFF9DBFA8)
    else -> Navy to NavyLight
}

private fun iconFor(key: String): ImageVector = when (key.lowercase()) {
    "curso" -> Icons.Filled.School
    "libro" -> Icons.Filled.AutoStories
    "material", "articulo" -> Icons.Filled.Inventory2
    else -> Icons.Filled.AccountBalance
}

/**
 * Imagen referencial: muestra la foto si existe (URL o archivo tomado con la cámara)
 * o un degradado con ícono según la categoría.
 */
@Composable
fun ReferenceImage(imageUrl: String, category: String, modifier: Modifier = Modifier, iconSize: Dp = 40.dp) {
    val (c1, c2) = paletteFor(category)
    Box(
        modifier.background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center
    ) {
        Icon(iconFor(category), null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(iconSize))
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = if (imageUrl.startsWith("/")) File(imageUrl) else imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private val avatarColors = listOf(Color(0xFFB8945A), Color(0xFF2A8C82), Color(0xFF1D4269), Color(0xFF8E4A5C), Color(0xFF5C7A3E))

/** Avatar ficticio predefinido ("avatar_N") o foto tomada con la cámara. */
@Composable
fun Avatar(avatar: String, size: Dp = 48.dp, name: String = "", selected: Boolean = false) {
    val idx = avatar.removePrefix("avatar_").toIntOrNull()
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColors[((idx ?: 1) - 1).mod(avatarColors.size)])
            .then(if (selected) Modifier.border(3.dp, Navy, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (idx == null && avatar.isNotBlank()) {
            AsyncImage(File(avatar), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else if (name.isNotBlank()) {
            Text(name.trim().first().uppercase(), color = Color.White, fontWeight = FontWeight.Bold,
                style = if (size > 60.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium)
        } else {
            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
        }
    }
}

@Composable
fun Tag(text: String, container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(text, color = content, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun Rating(value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            Icon(Icons.Filled.Star, null, tint = if (i < value.toInt()) Gold else Color(0xFFDDD5C7), modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(String.format(java.util.Locale.US, "%.1f", value), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Aviso cuando el backend no responde y se está usando la caché SQLite. */
@Composable
fun OfflineBanner(online: Boolean?) {
    if (online == false) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFFFF4E0)).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.CloudOff, null, tint = Color(0xFF9A6B00), modifier = Modifier.size(18.dp))
            Text("Sin conexión al servidor: mostrando datos guardados en el teléfono",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B4A00))
        }
    }
}

/** Animación de éxito (check que aparece con rebote). */
@Composable
fun SuccessCheck(size: Dp = 96.dp, color: Color = Teal) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "check"
    )
    LaunchedEffect(Unit) { started = true }
    Box(Modifier.size(size).scale(scale).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(size * 0.65f))
    }
}

fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

/** Guarda una foto de la cámara en almacenamiento interno y devuelve la ruta. */
fun saveBitmap(context: Context, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return file.absolutePath
}

/**
 * Lanza la cámara (foto rápida) pidiendo antes el permiso CAMERA.
 * Devuelve una función que se llama desde un botón.
 */
@Composable
fun rememberCameraCapture(onPhoto: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) onPhoto(saveBitmap(context, bmp))
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePicture.launch(null) else toast(context, "Se necesita permiso de cámara")
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            takePicture.launch(null)
        else permission.launch(Manifest.permission.CAMERA)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

fun statusColors(status: String): Pair<Color, Color> = when (status) {
    "ENTREGADO" -> Color(0xFFD9F2E3) to Color(0xFF1E6B3D)
    "ENVIADO" -> Color(0xFFDCE6F7) to Color(0xFF274C8A)
    "CONFIRMADO" -> Color(0xFFFFEFC7) to Color(0xFF8A5A00)
    else -> Color(0xFFF7E3D0) to Color(0xFF8A4500)
}
