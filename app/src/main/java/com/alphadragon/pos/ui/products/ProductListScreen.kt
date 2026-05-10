package com.alphadragon.pos.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.theme.BrandRed
import com.alphadragon.pos.ui.theme.SurfaceContainer
import com.alphadragon.pos.R

@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onProductClick: (String) -> Unit,
    onManageCategories: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.deleteConfirmProductId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete product?") },
            text = { Text("This product will be deactivated and hidden from the POS.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            AlphaDragonTopBar(title = "Products", onBack = onBack, actions = {
                IconButton(onClick = viewModel::toggleViewType) {
                    Icon(
                        imageVector = if (state.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = if (state.isGridView) "Switch to list" else "Switch to grid"
                    )
                }
                IconButton(onClick = onManageCategories) {
                    Icon(
                        painter = painterResource(id = R.drawable.category),
                        contentDescription = "Categories"
                    )
                }
           
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                containerColor = BrandRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search products…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (state.categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
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

            if (state.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No products found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state.isGridView) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val cols = (maxWidth / 160.dp).toInt().coerceAtLeast(2)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.products, key = { it.id }) { product ->
                            ProductGridCard(
                                product = product,
                                onClick = { onProductClick(product.id) },
                                onDelete = { viewModel.requestDeleteConfirm(product.id) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductListCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onDelete = { viewModel.requestDeleteConfirm(product.id) }
                        )
                    }
                }
            }
        }
    }
}

// ── Shared image composable ───────────────────────────────────────────────────

@Composable
fun ProductImage(
    imagePath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (imagePath != null) {
        AsyncImage(
            model = java.io.File(imagePath),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ImageNotSupported,
                contentDescription = "No image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ── Grid card ────────────────────────────────────────────────────────────────

@Composable
private fun ProductGridCard(product: Product, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column {
                // Image area — 60% of card height
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.1f)) {
                    ProductImage(
                        imagePath = product.imagePath,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay at bottom so text is readable on any image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                    )
                    if (product.isLowStock) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        ) { Text("Low stock") }
                    }
                }

                // Info area
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "£%.2f".format(product.price),
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandRed,
                            fontWeight = FontWeight.Bold
                        )
                        if (product.sku != null) {
                            Text(
                                product.sku!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Delete button — top-right corner overlay
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── List card ────────────────────────────────────────────────────────────────

@Composable
private fun ProductListCard(product: Product, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            ProductImage(
                imagePath = product.imagePath,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
                contentScale = ContentScale.Crop
            )

            // Text info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                val detail = buildString {
                    if (product.sku != null) append("SKU: ${product.sku}  ")
                    if (product.taxRate != null) append("Tax: ${product.taxRate}%")
                }
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("£%.2f".format(product.price), style = MaterialTheme.typography.titleSmall, color = BrandRed, fontWeight = FontWeight.Bold)
                    if (product.isLowStock) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) { Text("Low stock") }
                    }
                }
            }

            // Delete
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
