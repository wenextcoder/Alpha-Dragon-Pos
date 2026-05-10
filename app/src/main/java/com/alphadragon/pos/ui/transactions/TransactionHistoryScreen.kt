package com.alphadragon.pos.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.PaymentMethod
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionStatus
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun TransactionHistoryScreen(
    onTransactionClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { AlphaDragonTopBar("Transactions", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search by name, phone or ID…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Payment method filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.paymentFilter == null,
                        onClick = { viewModel.setPaymentFilter(null) },
                        label = { Text("All") }
                    )
                }
                items(PaymentMethod.entries) { method ->
                    FilterChip(
                        selected = state.paymentFilter == method,
                        onClick = {
                            viewModel.setPaymentFilter(if (state.paymentFilter == method) null else method)
                        },
                        label = { Text(method.value.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Status filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.statusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("Any status") }
                    )
                }
                val statuses = listOf(
                    TransactionStatus.COMPLETED,
                    TransactionStatus.REFUNDED,
                    TransactionStatus.VOIDED
                )
                items(statuses) { status ->
                    FilterChip(
                        selected = state.statusFilter == status,
                        onClick = {
                            viewModel.setStatusFilter(if (state.statusFilter == status) null else status)
                        },
                        label = { Text(status.value.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            HorizontalDivider()

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.transactions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionRow(tx = tx, onClick = { onTransactionClick(tx.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val fmt = NumberFormat.getCurrencyInstance()
    val dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    ListItem(
        headlineContent = {
            Text("${fmt.format(tx.total)} — ${tx.paymentMethod.value.replaceFirstChar { it.uppercase() }}")
        },
        supportingContent = {
            Column {
                Text(dateFmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.bodySmall)
                if (tx.customerName != null) Text("Customer: ${tx.customerName}", style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Text(
                tx.status.value.replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = when (tx.status) {
                    TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    TransactionStatus.VOIDED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
