package com.alphadragon.pos.ui.sale

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.CartItem
import com.alphadragon.pos.domain.model.Customer
import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.currency.rememberCurrencyFormatter
import com.alphadragon.pos.ui.products.ProductImage
import com.alphadragon.pos.ui.scanner.BarcodeScannerDialog
import com.alphadragon.pos.ui.theme.BrandRed
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    initialBarcode: String? = null,
    initialProductId: String? = null,
    onBack: (() -> Unit)? = null,
    viewModel: SaleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currency = rememberCurrencyFormatter()
    var showPaySheet by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCreateCustomer by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var initialBarcodeHandled by rememberSaveable(initialBarcode) { mutableStateOf(false) }
    var initialProductIdHandled by rememberSaveable(initialProductId) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.paymentComplete) {
        if (state.paymentComplete) viewModel.resetPaymentComplete()
    }

    LaunchedEffect(initialBarcode, initialBarcodeHandled) {
        if (!initialBarcode.isNullOrBlank() && !initialBarcodeHandled) {
            initialBarcodeHandled = true
            viewModel.scanBarcode(initialBarcode)
        }
    }

    LaunchedEffect(initialProductId, initialProductIdHandled) {
        if (!initialProductId.isNullOrBlank() && !initialProductIdHandled) {
            initialProductIdHandled = true
            viewModel.ensureProductInCartById(initialProductId)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan product",
            helperText = "Center the barcode inside the frame. The scanner will add the matching product automatically.",
            onBarcodeScanned = { barcode ->
                showBarcodeScanner = false
                viewModel.scanBarcode(barcode)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (showPaySheet) {
        PaymentMethodSheet(
            total = state.cartTotal,
            currency = currency,
            isLoading = state.isLoading,
            onCash = { showPaySheet = false; viewModel.payCash() },
            onDismiss = { showPaySheet = false }
        )
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            hasSelection = state.customerName.isNotBlank(),
            onSelect = { c ->
                viewModel.selectCustomer(c)
                showCustomerPicker = false
            },
            onClear = {
                viewModel.clearSaleCustomer()
                showCustomerPicker = false
            },
            onCreateNew = { showCreateCustomer = true },
            onDismiss = { showCustomerPicker = false }
        )
    }

    if (showCreateCustomer) {
        CreateCustomerDialog(
            onSave = { name, phone, email, note ->
                viewModel.createAndSelectCustomer(name, phone, email, note) { ok ->
                    if (ok) {
                        showCreateCustomer = false
                        showCustomerPicker = false
                    }
                }
            },
            onDismiss = { showCreateCustomer = false }
        )
    }

    Scaffold(
        topBar = {
            AlphaDragonTopBar(
                title = "Sale",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Top 60%: Product browser ──────────────────────────────────
            Column(modifier = Modifier.weight(if (state.cartItems.isEmpty()) 1f else 0.6f)) {
                // Search bar with barcode icon
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearch,
                    placeholder = { Text("Search by name, SKU or scan barcode…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { showBarcodeScanner = true }) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan barcode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                // Category chips
                if (state.categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedCategoryId == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text("All") }
                            )
                        }
                        items(state.categories) { cat ->
                            FilterChip(
                                selected = state.selectedCategoryId == cat.id,
                                onClick = { viewModel.selectCategory(cat.id) },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Responsive product grid or empty state
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    if (state.products.isEmpty()) {
                        SaleProductBrowserEmpty(
                            hasSearchQuery = state.searchQuery.isNotBlank(),
                            hasCategoryFilter = state.selectedCategoryId != null,
                            onScanBarcode = { showBarcodeScanner = true }
                        )
                    } else {
                        val columnCount = (maxWidth / 140.dp).toInt().coerceAtLeast(2)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.products, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    currency = currency,
                                    cartQty = state.cartItems.firstOrNull { it.product.id == product.id }?.quantity?.toInt() ?: 0,
                                    onClick = { viewModel.toggleProductSelection(product) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.cartItems.isNotEmpty()) {
                HorizontalDivider(thickness = 2.dp)

                // ── Bottom 40%: Cart ──────────────────────────────────────────
                Column(modifier = Modifier.weight(0.4f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cart (${state.itemCount})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (state.cartItems.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearCart) { Text("Clear") }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.cartItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            currency = currency,
                            onIncrease = { viewModel.increaseQty(item.product.id) },
                            onDecrease = { viewModel.decreaseQty(item.product.id) },
                            onRemove = { viewModel.removeFromCart(item.product.id) }
                        )
                    }
                }

                // Totals + action row
                Surface {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                            Text(currency.format(state.cartSubtotal), style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.cartTax > 0) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Tax", style = MaterialTheme.typography.bodySmall)
                                Text(currency.format(state.cartTax), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(currency.format(state.cartTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomerPicker = true },
                                modifier = Modifier.weight(1f),
                                enabled = state.cartItems.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (state.customerName.isNotBlank()) state.customerName else "Customer",
                                    maxLines = 1
                                )
                            }
                            Button(
                                onClick = { showPaySheet = true },
                                enabled = state.cartItems.isNotEmpty() && !state.isLoading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Pay ${currency.format(state.cartTotal)}", fontWeight = FontWeight.Bold)
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

@Composable
private fun SaleProductBrowserEmpty(
    hasSearchQuery: Boolean,
    hasCategoryFilter: Boolean,
    onScanBarcode: () -> Unit
) {
    val (title, body, icon) = when {
        hasSearchQuery ->
            Triple(
                "No matching products",
                "Try another search, clear the field to see your full list, or scan a barcode.",
                Icons.Default.Search
            )
        hasCategoryFilter ->
            Triple(
                "Nothing in this category",
                "Pick \"All\" or another category. You can organize products from the Products screen.",
                Icons.Default.Category
            )
        else ->
            Triple(
                "Nothing to sell yet",
                "Your catalog is empty. Open Products and tap + to add items, then come back here—or scan a barcode once products exist.",
                Icons.Default.Inventory
            )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onScanBarcode) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Scan barcode")
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    currency: NumberFormat,
    cartQty: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)) {
                ProductImage(
                    imagePath = product.imagePath,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                    contentScale = ContentScale.Crop
                )
                if (cartQty > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                        containerColor = BrandRed
                    ) { Text("$cartQty") }
                }
                if (product.isLowStock) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) { Text("Low") }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                Text(product.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = FontWeight.Medium)
                Text(
                    currency.format(product.price),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    currency: NumberFormat,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                ProductImage(
                    imagePath = item.product.imagePath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${currency.format(item.product.price)} each",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .widthIn(min = 40.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${item.quantity.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalIconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(18.dp))
                }
            }

            Text(
                currency.format(item.lineTotal),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 64.dp)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<Customer>,
    hasSelection: Boolean,
    onSelect: (Customer) -> Unit,
    onClear: () -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (customers.isEmpty()) {
                    Text(
                        "No saved customers yet. Create one to reuse on future sales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(customers, key = { it.id }) { c ->
                            TextButton(
                                onClick = { onSelect(c) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        c.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!c.email.isNullOrBlank()) {
                                        Text(
                                            c.email,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                OutlinedButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create new customer")
                }
                if (hasSelection) {
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear selection", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CreateCustomerDialog(
    onSave: (String, String, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (printed on receipt)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "* Required to save",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        phone,
                        email.trim().takeIf { it.isNotBlank() },
                        note.trim().takeIf { it.isNotBlank() }
                    )
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodSheet(
    total: Double,
    currency: NumberFormat,
    isLoading: Boolean,
    onCash: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Pay ${currency.format(total)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text("Select payment method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onCash,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                Icon(Icons.Default.Money, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cash", style = MaterialTheme.typography.titleSmall)
            }

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Card", style = MaterialTheme.typography.titleSmall)
                    Text("Phase 2 — merchant required", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.CallSplit, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Split", style = MaterialTheme.typography.titleSmall)
                    Text("Phase 2 — requires card terminal", style = MaterialTheme.typography.labelSmall)
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel")
            }
        }
    }
}

