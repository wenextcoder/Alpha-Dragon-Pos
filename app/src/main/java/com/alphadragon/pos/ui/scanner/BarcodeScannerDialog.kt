package com.alphadragon.pos.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.theme.BrandRed
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
    title: String,
    helperText: String,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val cameraAvailable = remember {
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionAsked = true
        hasPermission = granted
    }

    LaunchedEffect(cameraAvailable, hasPermission) {
        if (cameraAvailable && !hasPermission && !permissionAsked) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    AlphaDragonTopBar(
                        title = title,
                        onBack = onDismiss
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when {
                        !cameraAvailable -> CameraUnavailableMessage(onDismiss = onDismiss)
                        hasPermission -> BarcodeCameraPreview(
                            helperText = helperText,
                            onBarcodeScanned = onBarcodeScanned,
                            onDismiss = onDismiss,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> CameraPermissionMessage(
                            permissionAsked = permissionAsked,
                            onRequestPermission = {
                                permissionAsked = true
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraUnavailableMessage(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NoPhotography,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("No camera found", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "This device does not expose a camera for barcode scanning. You can still enter the barcode manually.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDismiss) { Text("Enter manually") }
    }
}

@Composable
private fun CameraPermissionMessage(
    permissionAsked: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = BrandRed
        )
        Spacer(Modifier.height(16.dp))
        Text("Camera permission needed", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Allow camera access to scan product barcodes. The camera feed stays on this device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            if (permissionAsked) {
                OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
            }
            Button(onClick = onRequestPermission) {
                Text("Allow camera")
            }
        }
    }
}

@Composable
private fun BarcodeCameraPreview(
    helperText: String,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analyzerBusy = remember { AtomicBoolean(false) }
    val scanDelivered = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            scanner.close()
            cameraExecutor.shutdown()
            cameraProviderFuture.addListener(
                { runCatching { cameraProviderFuture.get().unbindAll() } },
                mainExecutor
            )
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                    cameraProviderFuture.addListener(
                        {
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setTargetResolution(android.util.Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                                            scanImageProxy(
                                                imageProxy = imageProxy,
                                                scanner = scanner,
                                                analyzerBusy = analyzerBusy,
                                                scanDelivered = scanDelivered,
                                                onBarcodeScanned = { value ->
                                                    mainExecutor.execute { onBarcodeScanned(value) }
                                                }
                                            )
                                        }
                                    }

                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                                cameraError = null
                            }.onFailure {
                                cameraError = it.message ?: "Camera could not start"
                            }
                        },
                        mainExecutor
                    )
                }
            }
        )

        if (cameraError == null) {
            ScannerOverlay(
                helperText = helperText,
                torchEnabled = torchEnabled,
                flashAvailable = camera?.cameraInfo?.hasFlashUnit() == true,
                onToggleTorch = {
                    val next = !torchEnabled
                    camera?.cameraControl?.enableTorch(next)
                    torchEnabled = next
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CameraUnavailableMessage(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun ScannerOverlay(
    helperText: String,
    torchEnabled: Boolean,
    flashAvailable: Boolean,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "barcodeScanLine")
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barcodeScanLineOffset"
    )
    val density = LocalDensity.current
    val boxHeightPx = with(density) { 200.dp.toPx() }
    val dimColor = Color.Black.copy(alpha = 0.62f)
    val cornerArm = with(density) { 32.dp.toPx() }
    val strokeW = with(density) { 4.dp.toPx() }
    val scanInset = with(density) { 16.dp.toPx() }
    val scanLineW = with(density) { 3.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val boxW = w * 0.85f
            val boxH = minOf(boxHeightPx, h * 0.45f)
            val left = (w - boxW) / 2f
            val top = (h - boxH) / 2f
            val right = left + boxW
            val bottom = top + boxH

            drawRect(dimColor, topLeft = Offset.Zero, size = Size(w, top.coerceAtLeast(0f)))
            drawRect(dimColor, topLeft = Offset(0f, bottom), size = Size(w, (h - bottom).coerceAtLeast(0f)))
            drawRect(dimColor, topLeft = Offset(0f, top), size = Size(left.coerceAtLeast(0f), boxH))
            drawRect(dimColor, topLeft = Offset(right, top), size = Size((w - right).coerceAtLeast(0f), boxH))

            fun cornerL(x0: Float, y0: Float, dirX: Float, dirY: Float) {
                drawLine(BrandRed, Offset(x0, y0), Offset(x0 + cornerArm * dirX, y0), strokeWidth = strokeW)
                drawLine(BrandRed, Offset(x0, y0), Offset(x0, y0 + cornerArm * dirY), strokeWidth = strokeW)
            }
            cornerL(left, top, 1f, 1f)
            cornerL(right, top, -1f, 1f)
            cornerL(left, bottom, 1f, -1f)
            cornerL(right, bottom, -1f, -1f)

            val scanY = top + boxH * lineProgress
            drawLine(
                color = BrandRed.copy(alpha = 0.95f),
                start = Offset(left + scanInset, scanY),
                end = Offset(right - scanInset, scanY),
                strokeWidth = scanLineW
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = BrandRed)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Hold steady", fontWeight = FontWeight.SemiBold)
                    Text(
                        helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (flashAvailable) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                    contentDescription = if (torchEnabled) "Turn flash off" else "Turn flash on",
                    tint = BrandRed
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun scanImageProxy(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    analyzerBusy: AtomicBoolean,
    scanDelivered: AtomicBoolean,
    onBarcodeScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || scanDelivered.get() || !analyzerBusy.compareAndSet(false, true)) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val value = barcodes
                .asSequence()
                .mapNotNull { it.rawValue?.trim() }
                .firstOrNull(::isValidBarcodeValue)

            if (value != null && scanDelivered.compareAndSet(false, true)) {
                onBarcodeScanned(value)
            }
        }
        .addOnCompleteListener {
            analyzerBusy.set(false)
            imageProxy.close()
        }
}

private fun isValidBarcodeValue(value: String): Boolean {
    if (value.length !in 3..128) return false
    return value.any { it.isLetterOrDigit() } &&
        value.none { it.isISOControl() } &&
        value.all { it.isLetterOrDigit() || it in "-_.:/ " }
}
