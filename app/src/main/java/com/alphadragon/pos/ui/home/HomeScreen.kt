package com.alphadragon.pos.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.ui.theme.BrandRed

@Composable
fun HomeScreen(
    onNavigateToSale: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val showRootWarning by viewModel.showRootWarning.collectAsState()
    val deviceMismatch by viewModel.deviceMismatch.collectAsState()

    if (deviceMismatch) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Security Alert") },
            text = { Text("This device's hardware ID has changed since the POS was installed. This may indicate the app was moved to a different device. Proceed with caution — all data remains encrypted and this event has been logged.") },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeFingerprintMismatch() }) {
                    Text("I understand, continue")
                }
            }
        )
    }

    if (showRootWarning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Security Warning") },
            text = { Text("This device appears to be rooted. This may compromise the security of your POS data. You may continue, but this event has been logged.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissRootWarning() }) {
                    Text("I Understand, Continue")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Alpha Dragon POS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HomeMenuCard(
                    title = "New Sale",
                    icon = Icons.Default.ShoppingCart,
                    isPrimary = true,
                    onClick = onNavigateToSale
                )
            }
            item {
                HomeMenuCard(
                    title = "Products",
                    icon = Icons.Default.Inventory,
                    onClick = onNavigateToProducts
                )
            }
            item {
                HomeMenuCard(
                    title = "Transactions",
                    icon = Icons.Default.Receipt,
                    onClick = onNavigateToTransactions
                )
            }
            item {
                HomeMenuCard(
                    title = "Reports",
                    icon = Icons.Default.BarChart,
                    onClick = onNavigateToReports
                )
            }
        }
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) BrandRed
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
