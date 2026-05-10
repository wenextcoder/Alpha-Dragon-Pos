package com.alphadragon.pos.ui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.*
import com.alphadragon.pos.domain.repository.ProductRepository
import com.alphadragon.pos.domain.usecase.sale.RecordTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleUiState(
    val searchQuery: String = "",
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val cartDiscount: Double = 0.0,
    val isLoading: Boolean = false,
    val paymentComplete: Boolean = false,
    val errorMessage: String? = null
) {
    val cartSubtotal: Double get() = cartItems.sumOf { it.lineSubtotal }
    val cartTax: Double get() = cartItems.sumOf { it.lineTax }
    val cartItemDiscount: Double get() = cartItems.sumOf { it.discountAmount }
    val cartTotal: Double get() = cartSubtotal - cartItemDiscount + cartTax - cartDiscount
    val itemCount: Int get() = cartItems.sumOf { it.quantity.toInt() }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SaleViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val recordTransactionUseCase: RecordTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleUiState())
    val uiState: StateFlow<SaleUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            productRepository.observeTopLevelCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        val catId = _uiState.value.selectedCategoryId
                        if (catId != null) productRepository.observeProductsByCategory(catId)
                        else productRepository.observeActiveProducts()
                    } else {
                        productRepository.searchProducts(query)
                    }
                }
                .collect { products -> _uiState.value = _uiState.value.copy(products = products) }
        }
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // Barcode lookup: all-digits string of reasonable barcode length
        if (query.length in 8..14 && query.all { it.isDigit() }) {
            viewModelScope.launch {
                val product = productRepository.getProductByBarcode(query)
                if (product != null) {
                    addToCart(product)
                    _uiState.value = _uiState.value.copy(searchQuery = "")
                    searchQuery.value = ""
                    return@launch
                }
            }
        }
        searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, searchQuery = "")
        searchQuery.value = ""
    }

    fun addToCart(product: Product) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = current[index].quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        _uiState.value = _uiState.value.copy(cartItems = current)
    }

    fun increaseQty(productId: String) {
        updateCartItem(productId) { it.copy(quantity = it.quantity + 1) }
    }

    fun decreaseQty(productId: String) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
            } else {
                current.removeAt(index)
            }
        }
        _uiState.value = _uiState.value.copy(cartItems = current)
    }

    fun removeFromCart(productId: String) {
        _uiState.value = _uiState.value.copy(
            cartItems = _uiState.value.cartItems.filter { it.product.id != productId }
        )
    }

    fun setItemDiscount(productId: String, amount: Double) {
        updateCartItem(productId) { item ->
            val maxDiscount = item.lineSubtotal
            item.copy(discountAmount = amount.coerceIn(0.0, maxDiscount))
        }
    }

    fun updateCartDiscount(amount: Double) {
        _uiState.value = _uiState.value.copy(cartDiscount = amount.coerceAtLeast(0.0))
    }

    fun updateCustomerName(value: String) { _uiState.value = _uiState.value.copy(customerName = value) }
    fun updateCustomerPhone(value: String) { _uiState.value = _uiState.value.copy(customerPhone = value) }
    fun updateCustomerEmail(value: String) { _uiState.value = _uiState.value.copy(customerEmail = value) }
    fun updateCustomerAddress(value: String) { _uiState.value = _uiState.value.copy(customerAddress = value) }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            customerName = "", customerPhone = "", customerEmail = "",
            customerAddress = "", cartDiscount = 0.0
        )
    }

    fun payCash() = recordPayment(PaymentMethod.CASH)

    fun payCard() {
        // Phase 2 — merchant SDK required. No-op for now; UI disables this button.
    }

    fun paySplit() {
        // Phase 2 — requires card terminal. No-op for now; UI disables this button.
    }

    private fun recordPayment(method: PaymentMethod) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            recordTransactionUseCase(
                cartItems = state.cartItems,
                paymentMethod = method,
                customerName = state.customerName,
                customerPhone = state.customerPhone,
                customerEmail = state.customerEmail,
                customerAddress = state.customerAddress,
                cartDiscount = state.cartDiscount
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, paymentComplete = true,
                        cartItems = emptyList(),
                        customerName = "", customerPhone = "", customerEmail = "",
                        customerAddress = "", cartDiscount = 0.0
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message ?: "Payment failed")
                }
            )
        }
    }

    fun resetPaymentComplete() {
        _uiState.value = _uiState.value.copy(paymentComplete = false)
    }

    private fun updateCartItem(productId: String, transform: (CartItem) -> CartItem) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) current[index] = transform(current[index])
        _uiState.value = _uiState.value.copy(cartItems = current)
    }
}
