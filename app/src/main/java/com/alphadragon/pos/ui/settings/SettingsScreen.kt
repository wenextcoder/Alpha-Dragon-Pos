package com.alphadragon.pos.ui.settings

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.currency.SupportedCurrencies

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onShopProfile: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    val state by viewModel.uiState.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }

    if (showCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            title = { Text("Shop currency") },
            text = {
                Column {
                    SupportedCurrencies.forEach { currencyCode ->
                        ListItem(
                            headlineContent = { Text(currencyCode) },
                            trailingContent = {
                                if (currencyCode == state.currencyCode) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCurrency(currencyCode)
                                    showCurrencyPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyPicker = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = { AlphaDragonTopBar("Settings", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsGroup("Shop") {
                    SettingsItem(
                        icon = Icons.Default.Store,
                        title = "Shop Profile",
                        subtitle = "Name, logo, address, currency",
                        onClick = onShopProfile
                    )
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        subtitle = state.currencyCode,
                        onClick = { showCurrencyPicker = true }
                    )
                    SettingsItem(icon = Icons.Default.Receipt, title = "Receipt Template", subtitle = "Header, footer, field visibility") {}
                    SettingsItem(icon = Icons.Default.Percent, title = "Tax Rules", subtitle = "Global rate and category overrides") {}
                }
            }
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            item {
                SettingsGroup("Security") {
                    SettingsItem(icon = Icons.Default.Lock, title = "Change PIN", subtitle = "Update your 6-digit admin PIN") {}
                    SettingsItem(icon = Icons.Default.Timer, title = "Session Timeout", subtitle = "Auto-logout after inactivity") {}
                    SettingsItem(icon = Icons.Default.History, title = "Audit Log", subtitle = "View security and activity events") {}
                }
            }
            item {
                SettingsGroup("Payments (Phase 2)") {
                    SettingsItem(icon = Icons.Default.CreditCard, title = "Merchant Config", subtitle = "API keys for payment processors", enabled = false) {}
                    SettingsItem(icon = Icons.Default.Devices, title = "Terminal Pairing", subtitle = "Connect POS terminals", enabled = false) {}
                }
            }
            item {
                SettingsGroup("Data") {
                    SettingsItem(icon = Icons.Default.Backup, title = "Backup & Restore", subtitle = "Export or import encrypted .adb file") {}
                }
            }
            item {
                SettingsGroup("App") {
                    SettingsItem(icon = Icons.Default.Info, title = "About Alpha Dragon", subtitle = "Version and device info") {}
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider()
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = if (enabled) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    )
}
