package com.alphadragon.pos.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.model.PaymentMethod
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionStatus
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.currency.rememberCurrencyFormatter
import java.text.DateFormat
import java.util.Date

@Composable
fun TransactionHistoryScreen(
    onTransactionClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        TransactionFilterDialog(
            paymentFilter = state.paymentFilter,
            statusFilter = state.statusFilter,
            categoryFilter = state.categoryFilter,
            categories = state.categories,
            onPaymentSelected = viewModel::setPaymentFilter,
            onStatusSelected = viewModel::setStatusFilter,
            onCategorySelected = viewModel::setCategoryFilter,
            onClear = viewModel::clearFilters,
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        topBar = {
            AlphaDragonTopBar(
                "Transactions",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter transactions")
                    }
                }
            )
        }
    ) { padding ->
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

            HorizontalDivider()

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.transactions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionRow(
                            tx = tx,
                            onClick = { onTransactionClick(tx.id) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = rememberCurrencyFormatter()
    val dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = {
                Text(fmt.format(tx.total), fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Column {
                    Text(dateFmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.bodySmall)
                    Text(
                        tx.customerName?.let { "Customer: $it" } ?: "Walk-in customer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            overlineContent = {
                Text(tx.paymentMethod.value.replaceFirstChar { it.uppercase() })
            },
            trailingContent = {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(tx.status.value.replace('_', ' ').replaceFirstChar { it.uppercase() })
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = when (tx.status) {
                            TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            TransactionStatus.VOIDED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }
        )
    }
}

@Composable
private fun TransactionFilterDialog(
    paymentFilter: PaymentMethod?,
    statusFilter: TransactionStatus?,
    categoryFilter: Category?,
    categories: List<Category>,
    onPaymentSelected: (PaymentMethod?) -> Unit,
    onStatusSelected: (TransactionStatus?) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter transactions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterSection(title = "Payment") {
                    FilterChip(selected = paymentFilter == null, onClick = { onPaymentSelected(null) }, label = { Text("All") })
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = paymentFilter == method,
                            onClick = { onPaymentSelected(if (paymentFilter == method) null else method) },
                            label = { Text(method.value.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                FilterSection(title = "Status") {
                    FilterChip(selected = statusFilter == null, onClick = { onStatusSelected(null) }, label = { Text("Any") })
                    listOf(TransactionStatus.COMPLETED, TransactionStatus.REFUNDED, TransactionStatus.VOIDED).forEach { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { onStatusSelected(if (statusFilter == status) null else status) },
                            label = { Text(status.value.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                FilterSection(title = "Product category") {
                    FilterChip(selected = categoryFilter == null, onClick = { onCategorySelected(null) }, label = { Text("All") })
                    categories.forEach { category ->
                        FilterChip(
                            selected = categoryFilter?.id == category.id,
                            onClick = { onCategorySelected(if (categoryFilter?.id == category.id) null else category) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onClear) { Text("Clear") } }
    )
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
