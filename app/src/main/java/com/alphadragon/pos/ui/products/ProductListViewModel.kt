package com.alphadragon.pos.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.domain.repository.ProductRepository
import com.alphadragon.pos.domain.usecase.product.DeleteProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val isGridView: Boolean = true,
    val isLoading: Boolean = false,
    val deleteConfirmProductId: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            productRepository.observeTopLevelCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
        viewModelScope.launch {
            searchQuery.debounce(300)
                .flatMapLatest { q ->
                    if (q.isBlank()) productRepository.observeActiveProducts()
                    else productRepository.searchProducts(q)
                }
                .collect { products ->
                    _uiState.value = _uiState.value.copy(allProducts = products)
                    applyFilters()
                }
        }
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        applyFilters()
    }

    fun toggleViewType() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun requestDeleteConfirm(productId: String) {
        _uiState.value = _uiState.value.copy(deleteConfirmProductId = productId)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(deleteConfirmProductId = null)
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmProductId ?: return
        _uiState.value = _uiState.value.copy(deleteConfirmProductId = null)
        viewModelScope.launch { deleteProductUseCase(id) }
    }

    private fun applyFilters() {
        val catId = _uiState.value.selectedCategoryId
        val filtered = if (catId == null) _uiState.value.allProducts
        else _uiState.value.allProducts.filter { it.categoryId == catId }
        _uiState.value = _uiState.value.copy(products = filtered)
    }
}
