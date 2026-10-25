package cl.duoc.museo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.museo.ui.components.MuseoTopBar
import cl.duoc.museo.ui.components.ReferenceImage
import cl.duoc.museo.ui.components.SuccessCheck
import cl.duoc.museo.ui.components.Tag
import cl.duoc.museo.ui.components.toast
import cl.duoc.museo.ui.theme.Danger
import cl.duoc.museo.ui.theme.Gold
import cl.duoc.museo.ui.theme.Navy
import cl.duoc.museo.ui.viewmodel.AppViewModelProvider
import cl.duoc.museo.ui.viewmodel.QrResultViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Analizador de cuadros de la cámara con ML Kit (solo códigos QR). */
private class QrAnalyzer(private val onFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image ?: run { proxy.close(); return }
        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onFound) }
            .addOnCompleteListener { proxy.close() }
    }
}

// =============== 7. Escáner QR ===============

@Composable
fun QrScannerScreen(onBack: () -> Unit, onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val handled = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val deliver: (String) -> Unit = { code -> if (handled.compareAndSet(false, true)) onCode(code) }

    // Lectura desde una imagen de la galería
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
                ?: return@rememberLauncherForActivityResult toast(context, "No se pudo abrir la imagen")
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { codes ->
                    val code = codes.firstNotNullOfOrNull { it.rawValue }
                    if (code != null) deliver(code) else toast(context, "No se encontró un código QR en la imagen")
                }
                .addOnFailureListener { toast(context, "No se pudo leer la imagen") }
        }
    }

    DisposableEffect(Unit) {
        if (!granted) permission.launch(Manifest.permission.CAMERA)
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            executor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(executor, QrAnalyzer { code -> previewView.post { deliver(code) } }) }
                        runCatching {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        }.onFailure { toast(ctx, "No se pudo iniciar la cámara") }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        } else {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Necesitamos acceso a la cámara para leer códigos QR", color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
            }
        }

        // Encabezado
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { manual = true }) { Icon(Icons.Filled.Keyboard, "Ingresar código", tint = Color.White) }
            }
            Text("Escáner QR", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Escanea códigos de museos, publicaciones o productos.", color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        // Marco de lectura
        Box(
            Modifier.align(Alignment.Center).size(250.dp)
                .border(BorderStroke(3.dp, Color.White), RoundedCornerShape(24.dp))
        )

        // Controles inferiores
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                ScannerButton(if (torch) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn, "Linterna") {
                    torch = !torch
                    camera?.cameraControl?.enableTorch(torch)
                }
                ScannerButton(Icons.Filled.PhotoLibrary, "Desde galería") {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Coloca el código dentro del marco y se escaneará automáticamente.",
                color = Color.White, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)).padding(12.dp)
            )
        }
    }

    if (manual) {
        var text by remember { mutableStateOf("MUSEO:1") }
        AlertDialog(
            onDismissRequest = { manual = false },
            title = { Text("Ingresar código manualmente") },
            text = {
                Column {
                    Text("Útil para probar en el emulador. Formato: MUSEO:<id> o PROD:<id>", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(text, { text = it }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { manual = false; deliver(text) }) { Text("Validar") } },
            dismissButton = { TextButton(onClick = { manual = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ScannerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

// =============== 8. Resultado del QR ===============

@Composable
fun QrResultScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onScanAgain: () -> Unit) {
    val vm: QrResultViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { MuseoTopBar("Resultado del QR", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.loading -> {
                    Spacer(Modifier.height(80.dp)); CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp)); Text("Validando código…")
                }
                state.result != null -> {
                    val r = state.result!!
                    Spacer(Modifier.height(12.dp))
                    SuccessCheck()
                    Spacer(Modifier.height(16.dp))
                    Text("¡Código identificado!", style = MaterialTheme.typography.headlineSmall)
                    Text("Hemos encontrado la siguiente información:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp)) {
                            ReferenceImage(r.imageUrl, if (r.type == "MUSEUM") "Historia" else r.label, Modifier.size(width = 90.dp, height = 120.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(r.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Tag(r.label)
                                Spacer(Modifier.height(8.dp))
                                Text(r.description, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            }
                        }
                    }
                    Text("Consulta registrada en tu historial", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { onOpen(r.route) }, modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                        Text("Ver detalle"); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Volver a escanear")
                    }
                }
                else -> {
                    Spacer(Modifier.height(40.dp))
                    Icon(Icons.Filled.Error, null, tint = Danger, modifier = Modifier.size(96.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Código no válido", style = MaterialTheme.typography.headlineSmall)
                    Text(state.error ?: "Código desconocido", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy)) {
                        Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Volver a escanear")
                    }
                }
            }
        }
    }
}
