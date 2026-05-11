package com.alphadragon.pos.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.PaymentMethod
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionItem
import com.alphadragon.pos.domain.model.TransactionStatus
import com.alphadragon.pos.domain.repository.TransactionRepository
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.currency.rememberCurrencyFormatter
import com.alphadragon.pos.ui.theme.BrandRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val items: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val transactionId: String = requireNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tx = transactionRepository.getById(transactionId)
            val items = transactionRepository.getItemsForTransaction(transactionId)
            _uiState.value = TransactionDetailUiState(transaction = tx, items = items, isLoading = false)
        }
    }
}

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currency = rememberCurrencyFormatter()

    Scaffold(topBar = { AlphaDragonTopBar("Transaction details", onBack = onBack) }) { padding ->
        if (state.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            val tx = state.transaction
            if (tx == null) {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Transaction not found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TransactionHeaderCard(
                            transactionId = tx.id,
                            timestamp = tx.timestamp,
                            receiptRef = tx.receiptRef,
                            status = tx.status
                        )
                    }

                    item {
                        TotalsCard(transaction = tx, currency = currency)
                    }

                    if (hasCustomerInfo(tx)) {
                        item {
                            CustomerInfoCard(transaction = tx)
                        }
                    }

                    item {
                        PaymentInfoCard(transaction = tx)
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Line items (${state.items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    items(state.items, key = { it.id }) { line ->
                        LineItemCard(item = line, currency = currency)
                    }
                }
            }
        }
    }
}

private fun hasCustomerInfo(tx: Transaction): Boolean =
    !tx.customerName.isNullOrBlank() ||
        !tx.customerPhone.isNullOrBlank() ||
        !tx.customerEmail.isNullOrBlank() ||
        !tx.customerAddress.isNullOrBlank()

private fun TransactionStatus.toDisplayString(): String =
    value.split('_').joinToString(" ") { word ->
        word.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }

@Composable
private fun TransactionHeaderCard(
    transactionId: String,
    timestamp: Long,
    receiptRef: String?,
    status: TransactionStatus
) {
    val dateFmt = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
    val timeFmt = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    val date = Date(timestamp)
    val (statusBg, statusFg) = when (status) {
        TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TransactionStatus.REFUNDED, TransactionStatus.PARTIAL_REFUND ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TransactionStatus.VOIDED ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TransactionStatus.PENDING_REFUND ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        dateFmt.format(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        timeFmt.format(date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        status.toDisplayString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusFg
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Text(
                "ID · ${transactionId}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!receiptRef.isNullOrBlank()) {
                Text(
                    "Receipt ref · $receiptRef",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TotalsCard(transaction: Transaction, currency: java.text.NumberFormat) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Amounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            DetailRow(label = "Subtotal", value = currency.format(transaction.subtotal))
            if (transaction.discountTotal > 0.0) {
                DetailRow(
                    label = "Discount",
                    value = "-${currency.format(transaction.discountTotal)}",
                    valueColor = MaterialTheme.colorScheme.error
                )
            }
            if (transaction.taxTotal > 0.0) {
                DetailRow(label = "Tax", value = currency.format(transaction.taxTotal))
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DetailRow(
                label = "Total paid",
                value = currency.format(transaction.total),
                emphasize = true,
                valueColor = BrandRed
            )
        }
    }
}

@Composable
private fun CustomerInfoCard(transaction: Transaction) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Customer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            transaction.customerName?.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Name", value = it)
            }
            transaction.customerPhone?.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Phone", value = it)
            }
            transaction.customerEmail?.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Email", value = it)
            }
            transaction.customerAddress?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PaymentInfoCard(transaction: Transaction) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Payment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            DetailRow(
                label = "Method",
                value = transaction.paymentMethod.toDisplayLabel()
            )
            transaction.merchantId?.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Merchant ID", value = it, mono = true)
            }
            transaction.terminalId?.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Terminal ID", value = it, mono = true)
            }
            transaction.approvalCode?.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Approval code", value = it, mono = true)
            }
            transaction.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun PaymentMethod.toDisplayLabel(): String = when (this) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.CARD -> "Card"
    PaymentMethod.SPLIT -> "Split"
}

@Composable
private fun LineItemCard(item: TransactionItem, currency: java.text.NumberFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${item.quantity.toInt()} × ${currency.format(item.unitPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.discount > 0) {
                Text(
                    "Discount −${currency.format(item.discount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (item.taxAmount > 0) {
                Text(
                    "Tax ${currency.format(item.taxAmount)} (${formatTaxRate(item.taxRate)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    currency.format(item.lineTotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatTaxRate(rate: Double): String {
    val pct = rate * 100.0
    return if (pct % 1.0 == 0.0) "${pct.toInt()}%" else "${"%.2f".format(pct)}%"
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    valueColor: Color? = null,
    mono: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
