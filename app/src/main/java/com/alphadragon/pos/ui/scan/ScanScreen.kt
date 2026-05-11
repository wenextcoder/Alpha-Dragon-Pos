package com.alphadragon.pos.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.scanner.BarcodeScannerDialog

@Composable
fun ScanScreen(
    onOpenSale: () -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        BarcodeScannerDialog(
            title = "Scan product",
            helperText = "Center the barcode inside the frame, then use Sale to add the matching product.",
            onBarcodeScanned = {
                showScanner = false
                onOpenSale()
            },
            onDismiss = { showScanner = false }
        )
    }

    Scaffold(
        topBar = {
            AlphaDragonTopBar(
                title = "Scan",
                actions = {
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.height(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Scan Products",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Use the scanner when you are ready. Sale also has barcode search and an in-screen scanner for adding products to the cart.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { showScanner = true }) {
                        Text("Scan barcode")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenSale) {
                        Text("Open Sale")
                    }
                }
            }
        }
    }
}
