package com.alphadragon.pos.ui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.*
import com.alphadragon.pos.domain.repository.CustomerRepository
import com.alphadragon.pos.domain.repository.ProductRepository
import com.alphadragon.pos.domain.usecase.sale.RecordTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SaleUiState(
    val searchQuery: String = "",
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val customers: List<Customer> = emptyList(),
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
    private val customerRepository: CustomerRepository,
    private val recordTransactionUseCase: RecordTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleUiState())
    val uiState: StateFlow<SaleUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            customerRepository.observeAll().collect { list ->
                _uiState.value = _uiState.value.copy(customers = list)
            }
        }
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
        _uiState.value = _uiState.value.copy(searchQuery = query, errorMessage = null)
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

    fun scanBarcode(barcode: String) {
        val normalized = barcode.trim()
        if (!normalized.isValidBarcodeValue()) {
            _uiState.value = _uiState.value.copy(errorMessage = "That barcode does not look valid")
            return
        }

        _uiState.value = _uiState.value.copy(searchQuery = normalized, errorMessage = null)
        searchQuery.value = normalized
        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(normalized)
            if (product != null) {
                addToCart(product)
                _uiState.value = _uiState.value.copy(searchQuery = "")
                searchQuery.value = ""
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "No product found for barcode $normalized")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
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

    fun toggleProductSelection(product: Product) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        _uiState.value = _uiState.value.copy(cartItems = current)
    }

    /** Adds product with quantity 1 if not already in cart (used after barcode scan). */
    fun ensureProductInCart(product: Product) {
        val current = _uiState.value.cartItems.toMutableList()
        if (current.none { it.product.id == product.id }) {
            current.add(CartItem(product = product, quantity = 1.0))
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun ensureProductInCartById(productId: String) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId) ?: return@launch
            ensureProductInCart(product)
        }
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

    fun selectCustomer(customer: Customer) {
        _uiState.value = _uiState.value.copy(
            customerName = customer.name,
            customerPhone = customer.phone,
            customerEmail = customer.email.orEmpty(),
            customerAddress = customer.note.orEmpty(),
            errorMessage = null
        )
    }

    fun clearSaleCustomer() {
        _uiState.value = _uiState.value.copy(
            customerName = "",
            customerPhone = "",
            customerEmail = "",
            customerAddress = ""
        )
    }

    fun createAndSelectCustomer(
        name: String,
        phone: String,
        email: String?,
        note: String?,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank() || phone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name and phone are required")
            onResult(false)
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                email = email?.trim()?.takeIf { it.isNotBlank() },
                note = note?.trim()?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now
            )
            customerRepository.insert(customer).fold(
                onSuccess = {
                    selectCustomer(customer)
                    onResult(true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Could not save customer")
                    onResult(false)
                }
            )
        }
    }

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

    private fun String.isValidBarcodeValue(): Boolean {
        if (length !in 3..128) return false
        return any { it.isLetterOrDigit() } &&
            none { it.isISOControl() } &&
            all { it.isLetterOrDigit() || it in "-_.:/ " }
    }
}
