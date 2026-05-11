package com.alphadragon.pos.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.model.PaymentMethod
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionStatus
import com.alphadragon.pos.domain.repository.ProductRepository
import com.alphadragon.pos.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionHistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val paymentFilter: PaymentMethod? = null,
    val statusFilter: TransactionStatus? = null,
    val categoryFilter: Category? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)

private data class TransactionFilters(
    val query: String,
    val payment: PaymentMethod?,
    val status: TransactionStatus?,
    val category: Category?
)

@OptIn(FlowPreview::class)
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _paymentFilter = MutableStateFlow<PaymentMethod?>(null)
    private val _statusFilter = MutableStateFlow<TransactionStatus?>(null)
    private val _categoryFilter = MutableStateFlow<Category?>(null)
    private val _transactionCategoryIds = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val categories = productRepository.observeTopLevelCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            transactionRepository.observeAll().collect { transactions ->
                _transactionCategoryIds.value = transactions.associate { transaction ->
                    val categoryIds = transactionRepository.getItemsForTransaction(transaction.id)
                        .mapNotNull { item -> item.productId }
                        .mapNotNull { productId -> productRepository.getProductById(productId)?.categoryId }
                        .toSet()
                    transaction.id to categoryIds
                }
            }
        }
    }

    private val filters = combine(
        _searchQuery.debounce(300),
        _paymentFilter,
        _statusFilter,
        _categoryFilter
    ) { query, payment, status, category ->
        TransactionFilters(
            query = query,
            payment = payment,
            status = status,
            category = category
        )
    }

    val uiState: StateFlow<TransactionHistoryUiState> = combine(
        transactionRepository.observeAll(),
        filters,
        categories,
        _transactionCategoryIds
    ) { allTx, filters, categories, transactionCategoryIds ->
        val filtered = allTx
            .filter { tx ->
                filters.query.isBlank() ||
                    tx.customerName?.contains(filters.query, ignoreCase = true) == true ||
                    tx.customerPhone?.contains(filters.query) == true ||
                    tx.id.startsWith(filters.query, ignoreCase = true)
            }
            .filter { tx -> filters.payment == null || tx.paymentMethod == filters.payment }
            .filter { tx -> filters.status == null || tx.status == filters.status }
            .filter { tx ->
                filters.category == null ||
                    transactionCategoryIds[tx.id]?.contains(filters.category.id) == true
            }

        TransactionHistoryUiState(
            transactions = filtered,
            searchQuery = filters.query,
            paymentFilter = filters.payment,
            statusFilter = filters.status,
            categoryFilter = filters.category,
            categories = categories
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TransactionHistoryUiState(isLoading = true)
    )

    fun updateSearch(query: String) { _searchQuery.value = query }
    fun setPaymentFilter(method: PaymentMethod?) { _paymentFilter.value = method }
    fun setStatusFilter(status: TransactionStatus?) { _statusFilter.value = status }
    fun setCategoryFilter(category: Category?) { _categoryFilter.value = category }

    fun clearFilters() {
        _paymentFilter.value = null
        _statusFilter.value = null
        _categoryFilter.value = null
    }
}
