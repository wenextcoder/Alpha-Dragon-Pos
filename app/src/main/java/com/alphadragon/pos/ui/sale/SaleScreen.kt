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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.CartItem
import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.products.ProductImage
import com.alphadragon.pos.ui.theme.BrandRed
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    onBack: () -> Unit,
    viewModel: SaleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showPaySheet by remember { mutableStateOf(false) }
    var showCustomerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.paymentComplete) {
        if (state.paymentComplete) viewModel.resetPaymentComplete()
    }

    if (showPaySheet) {
        PaymentMethodSheet(
            total = state.cartTotal,
            isLoading = state.isLoading,
            onCash = { showPaySheet = false; viewModel.payCash() },
            onDismiss = { showPaySheet = false }
        )
    }

    if (showCustomerSheet) {
        CustomerInfoSheet(
            name = state.customerName,
            phone = state.customerPhone,
            address = state.customerAddress,
            onNameChange = viewModel::updateCustomerName,
            onPhoneChange = viewModel::updateCustomerPhone,
            onAddressChange = viewModel::updateCustomerAddress,
            onDone = { showCustomerSheet = false },
            onDismiss = { showCustomerSheet = false }
        )
    }

    Scaffold(
        topBar = { AlphaDragonTopBar("New Sale", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Top 60%: Product browser ──────────────────────────────────
            Column(modifier = Modifier.weight(0.6f)) {
                // Search bar with barcode icon
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearch,
                    placeholder = { Text("Search by name, SKU or scan barcode…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan barcode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                // Responsive product grid
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                                cartQty = state.cartItems.firstOrNull { it.product.id == product.id }?.quantity?.toInt() ?: 0,
                                onClick = { viewModel.addToCart(product) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 2.dp)

            // ── Bottom 40%: Cart ──────────────────────────────────────────
            Column(modifier = Modifier.weight(0.4f)) {
                val fmt = NumberFormat.getCurrencyInstance()

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

                if (state.cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.cartItems, key = { it.product.id }) { item ->
                            CartItemRow(
                                item = item,
                                onIncrease = { viewModel.increaseQty(item.product.id) },
                                onDecrease = { viewModel.decreaseQty(item.product.id) },
                                onRemove = { viewModel.removeFromCart(item.product.id) },
                                onDiscountChange = { viewModel.setItemDiscount(item.product.id, it) }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                // Totals + action row
                Surface(tonalElevation = 2.dp) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                            Text(fmt.format(state.cartSubtotal), style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.cartItemDiscount > 0) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Discount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Text("-${fmt.format(state.cartItemDiscount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (state.cartTax > 0) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Tax", style = MaterialTheme.typography.bodySmall)
                                Text(fmt.format(state.cartTax), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(fmt.format(state.cartTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomerSheet = true },
                                modifier = Modifier.weight(1f),
                                enabled = state.cartItems.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (state.customerName.isNotBlank()) state.customerName
                                    else "+ Customer",
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
                                    Text("Pay ${fmt.format(state.cartTotal)}", fontWeight = FontWeight.Bold)
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
private fun ProductCard(product: Product, cartQty: Int, onClick: () -> Unit) {
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
                    "£%.2f".format(product.price),
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
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onDiscountChange: (Double) -> Unit
) {
    var discountText by remember(item.product.id) { mutableStateOf(if (item.discountAmount > 0) "%.2f".format(item.discountAmount) else "") }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text("£%.2f ea".format(item.product.price), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Quantity controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "−", modifier = Modifier.size(14.dp))
                }
                Text(
                    "${item.quantity.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 24.dp),
                )
                IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(14.dp))
                }
            }

            Text(
                "£%.2f".format(item.lineTotal),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 56.dp)
            )

            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
            }
        }

        // Per-item discount field
        OutlinedTextField(
            value = discountText,
            onValueChange = { v ->
                discountText = v
                onDiscountChange(v.toDoubleOrNull() ?: 0.0)
            },
            label = { Text("Discount £", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(52.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodSheet(
    total: Double,
    isLoading: Boolean,
    onCash: () -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = NumberFormat.getCurrencyInstance()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Pay ${fmt.format(total)}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerInfoSheet(
    name: String,
    phone: String,
    address: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onDone: () -> Unit,
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
            Text("Customer Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Used for invoice generation (optional)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                label = { Text("Address") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) { Text("Done") }
        }
    }
}
