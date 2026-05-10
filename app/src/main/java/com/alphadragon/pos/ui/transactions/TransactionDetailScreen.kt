package com.alphadragon.pos.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionItem
import com.alphadragon.pos.domain.repository.TransactionRepository
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

// ViewModel

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

// Screen

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { AlphaDragonTopBar("Transaction", onBack = onBack) }) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val tx = state.transaction
            if (tx == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Transaction not found")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    item {
                        val dateFmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        Text(dateFmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        if (tx.customerName != null) {
                            Text("Customer: ${tx.customerName}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Items", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(state.items) { item ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("${item.productName} × ${item.quantity.toInt()}", style = MaterialTheme.typography.bodyMedium)
                            Text("£%.2f".format(item.lineTotal))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text("£%.2f".format(tx.total), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Payment: ${tx.paymentMethod.value}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${tx.status.value}", style = MaterialTheme.typography.bodySmall)
                        if (tx.approvalCode != null) Text("Approval: ${tx.approvalCode}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
