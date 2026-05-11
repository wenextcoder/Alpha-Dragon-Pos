package com.alphadragon.pos.ui.products

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.alphadragon.pos.R
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.scanner.BarcodeScannerDialog
import com.alphadragon.pos.ui.theme.BrandRed
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan barcode",
            helperText = "Center the product barcode inside the frame to fill this field.",
            onBarcodeScanned = { barcode ->
                showBarcodeScanner = false
                viewModel.updateBarcode(barcode)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val cameraAvailable = remember {
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var imagePickerMessage by remember { mutableStateOf<String?>(null) }
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraCaptureUri
        if (success && uri != null) {
            viewModel.onImagePicked(uri)
        }
        cameraCaptureUri = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    fun buildCaptureUri(): Uri? = runCatching {
        val file = File.createTempFile("product_capture_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            imagePickerMessage = null
            val uri = buildCaptureUri()
            if (uri != null) {
                cameraCaptureUri = uri
                takePictureLauncher.launch(uri)
            } else {
                imagePickerMessage = "Could not prepare camera capture."
            }
        } else {
            imagePickerMessage = "Camera permission is needed to take a product photo."
        }
    }

    fun openCamera() {
        imagePickerMessage = null
        if (!cameraAvailable) {
            imagePickerMessage = "No camera is available on this device."
            return
        }
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                val uri = buildCaptureUri()
                if (uri != null) {
                    cameraCaptureUri = uri
                    takePictureLauncher.launch(uri)
                } else {
                    imagePickerMessage = "Could not prepare camera capture."
                }
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun openGallery() {
        imagePickerMessage = null
        galleryLauncher.launch("image/*")
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(enabled = cameraAvailable) {
                            showImageSourceDialog = false
                            openCamera()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (cameraAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                        Text(
                            "Camera",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (cameraAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            openGallery()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Image",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            AlphaDragonTopBar(
                title = if (productId == null) "Add Product" else "Edit Product",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isImageLoading) { showImageSourceDialog = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            state.isImageLoading -> CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                            state.imagePath != null -> {
                                AsyncImage(
                                    model = java.io.File(state.imagePath),
                                    contentDescription = "Product image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = viewModel::removeImage,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
                                }
                            }
                            else -> Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.placeholder_image),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.imagePath == null) "Add product image" else "Product image selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tap to choose a clear product photo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (imagePickerMessage != null) {
                            Text(
                                imagePickerMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showImageSourceDialog = true },
                            enabled = !state.isImageLoading,
                            modifier = Modifier
                                .height(36.dp)
                        ) {
                            Text(if (state.imagePath == null) "Choose image" else "Change")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Product name *") },
                singleLine = true,
                isError = state.errorMessage != null && state.name.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::updatePrice,
                label = { Text("Price *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            if (state.categories.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                val selectedCategoryName = state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "None"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { viewModel.updateCategoryId(null); expanded = false }
                        )
                        state.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { viewModel.updateCategoryId(cat.id); expanded = false }
                            )
                        }
                    }
                }
            }

            val errorText = state.errorMessage
            val barcodeDuplicate = errorText != null &&
                errorText.contains("already used by", ignoreCase = true)

            OutlinedTextField(
                value = state.barcode,
                onValueChange = viewModel::updateBarcode,
                label = { Text("Barcode (optional)") },
                singleLine = true,
                isError = barcodeDuplicate,
                supportingText = if (barcodeDuplicate && errorText != null) {
                    { Text(errorText, color = MaterialTheme.colorScheme.error) }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { showBarcodeScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stock tracking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Optional. Turn this on only when you want the POS to count stock.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = state.trackStock, onCheckedChange = viewModel::updateTrackStock)
                    }

                    if (state.trackStock) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.stockQty,
                            onValueChange = viewModel::updateStockQty,
                            label = { Text("Current stock quantity") },
                            supportingText = { Text("Leave as 0 if you will update stock later.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (showAdvanced) "Hide advanced" else "Advanced")
            }

            if (showAdvanced) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Details / description (optional)") },
                    supportingText = { Text("Internal notes about this product. Stored offline only.") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.taxRate,
                    onValueChange = viewModel::updateTaxRate,
                    label = { Text("Tax rate % (optional)") },
                    supportingText = { Text("Leave blank to use category or global tax rules.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.sku,
                    onValueChange = viewModel::updateSku,
                    label = { Text("Custom marking / naming (optional)") },
                    supportingText = { Text("Use this for a shelf code, short name, or internal marker.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.errorMessage != null && !barcodeDuplicate) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isLoading && state.name.isNotBlank() && state.price.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isEditMode) "Update Product" else "Add Product")
                }
            }
        }
    }
}
