package com.alphadragon.pos.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.PaymentMethod
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionStatus
import com.alphadragon.pos.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TransactionHistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val paymentFilter: PaymentMethod? = null,
    val statusFilter: TransactionStatus? = null,
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _paymentFilter = MutableStateFlow<PaymentMethod?>(null)
    private val _statusFilter = MutableStateFlow<TransactionStatus?>(null)

    val uiState: StateFlow<TransactionHistoryUiState> = combine(
        transactionRepository.observeAll(),
        _searchQuery.debounce(300),
        _paymentFilter,
        _statusFilter
    ) { allTx, query, payFilter, statusFilter ->
        val filtered = allTx
            .filter { tx ->
                query.isBlank() ||
                    tx.customerName?.contains(query, ignoreCase = true) == true ||
                    tx.customerPhone?.contains(query) == true ||
                    tx.id.startsWith(query, ignoreCase = true)
            }
            .filter { tx -> payFilter == null || tx.paymentMethod == payFilter }
            .filter { tx -> statusFilter == null || tx.status == statusFilter }

        TransactionHistoryUiState(
            transactions = filtered,
            searchQuery = query,
            paymentFilter = payFilter,
            statusFilter = statusFilter
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TransactionHistoryUiState(isLoading = true)
    )

    fun updateSearch(query: String) { _searchQuery.value = query }
    fun setPaymentFilter(method: PaymentMethod?) { _paymentFilter.value = method }
    fun setStatusFilter(status: TransactionStatus?) { _statusFilter.value = status }
}
