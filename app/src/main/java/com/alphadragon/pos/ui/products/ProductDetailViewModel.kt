package com.alphadragon.pos.ui.products

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.data.storage.ProductImageStorage
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.domain.repository.ProductRepository
import com.alphadragon.pos.domain.usecase.product.SaveProductUseCase
import com.alphadragon.pos.domain.usecase.product.UpdateProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val name: String = "",
    val price: String = "",
    val sku: String = "",
    val barcode: String = "",
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val taxRate: String = "",
    val imagePath: String? = null,       // absolute path inside filesDir — never a content:// URI
    val isImageLoading: Boolean = false,
    val trackStock: Boolean = false,
    val stockQty: String = "0",
    val isEditMode: Boolean = false
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val saveProductUseCase: SaveProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val imageStorage: ProductImageStorage
) : ViewModel() {

    private val productId: String? = savedStateHandle["productId"]

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.observeTopLevelCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
        if (productId != null) {
            viewModelScope.launch {
                val product = productRepository.getProductById(productId)
                if (product != null) {
                    _uiState.value = _uiState.value.copy(
                        isEditMode = true,
                        name = product.name,
                        price = product.price.toString(),
                        sku = product.sku ?: "",
                        barcode = product.barcode ?: "",
                        categoryId = product.categoryId,
                        taxRate = product.taxRate?.toString() ?: "",
                        imagePath = product.imagePath,
                        trackStock = product.trackStock,
                        stockQty = product.stockQty.toString()
                    )
                }
            }
        }
    }

    fun updateName(v: String) { _uiState.value = _uiState.value.copy(name = v, errorMessage = null) }
    fun updatePrice(v: String) { _uiState.value = _uiState.value.copy(price = v, errorMessage = null) }
    fun updateSku(v: String) { _uiState.value = _uiState.value.copy(sku = v) }
    fun updateBarcode(v: String) { _uiState.value = _uiState.value.copy(barcode = v) }
    fun updateCategoryId(v: String?) { _uiState.value = _uiState.value.copy(categoryId = v) }
    fun updateTaxRate(v: String) { _uiState.value = _uiState.value.copy(taxRate = v) }
    fun updateTrackStock(v: Boolean) { _uiState.value = _uiState.value.copy(trackStock = v) }
    fun updateStockQty(v: String) { _uiState.value = _uiState.value.copy(stockQty = v) }

    /**
     * Called when the user picks an image URI from the gallery.
     * Copies the image into private internal storage immediately, then updates the state
     * with the resulting absolute path. The original content:// URI is never persisted.
     */
    fun onImagePicked(uri: Uri) {
        val oldPath = _uiState.value.imagePath
        _uiState.value = _uiState.value.copy(isImageLoading = true)
        viewModelScope.launch {
            val newPath = imageStorage.copyFromUri(uri, replacingPath = oldPath)
            _uiState.value = _uiState.value.copy(
                imagePath = newPath,
                isImageLoading = false,
                errorMessage = if (newPath == null) "Failed to save image. Please try again." else null
            )
        }
    }

    /**
     * Removes the current image and deletes the file from private storage.
     */
    fun removeImage() {
        val oldPath = _uiState.value.imagePath
        if (oldPath != null) {
            viewModelScope.launch { imageStorage.deleteFile(oldPath) }
        }
        _uiState.value = _uiState.value.copy(imagePath = null)
    }

    fun save() {
        val state = _uiState.value
        val price = state.price.toDoubleOrNull()
        if (price == null) {
            _uiState.value = state.copy(errorMessage = "Invalid price")
            return
        }
        val taxRate = if (state.taxRate.isBlank()) null else state.taxRate.toDoubleOrNull()
        if (state.taxRate.isNotBlank() && taxRate == null) {
            _uiState.value = state.copy(errorMessage = "Invalid tax rate")
            return
        }
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        val now = System.currentTimeMillis()
        val product = Product(
            id = productId ?: UUID.randomUUID().toString(),
            name = state.name,
            sku = state.sku.takeIf { it.isNotBlank() },
            barcode = state.barcode.takeIf { it.isNotBlank() },
            categoryId = state.categoryId,
            price = price,
            taxRate = taxRate,
            imagePath = state.imagePath,
            trackStock = state.trackStock,
            stockQty = state.stockQty.toIntOrNull() ?: 0,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            val result = if (state.isEditMode) updateProductUseCase(product) else saveProductUseCase(product)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }
}
