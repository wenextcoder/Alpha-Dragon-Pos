package com.alphadragon.pos.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoryDetailUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val name: String = "",
    val taxRate: String = "",
    val displayOrder: String = "0",
    val isEditMode: Boolean = false
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val categoryId: String? = savedStateHandle["categoryId"]

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    init {
        if (categoryId != null) {
            viewModelScope.launch {
                // Observe category to pre-fill form
                productRepository.observeTopLevelCategories().collect { cats ->
                    val cat = cats.firstOrNull { it.id == categoryId }
                    if (cat != null && !_uiState.value.isEditMode) {
                        _uiState.value = _uiState.value.copy(
                            isEditMode = true,
                            name = cat.name,
                            taxRate = cat.taxRate?.toString() ?: "",
                            displayOrder = cat.displayOrder.toString()
                        )
                    }
                }
            }
        }
    }

    fun updateName(v: String) { _uiState.value = _uiState.value.copy(name = v, errorMessage = null) }
    fun updateTaxRate(v: String) { _uiState.value = _uiState.value.copy(taxRate = v) }
    fun updateDisplayOrder(v: String) { _uiState.value = _uiState.value.copy(displayOrder = v) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Category name cannot be empty")
            return
        }
        val taxRate = if (state.taxRate.isBlank()) null else state.taxRate.toDoubleOrNull()
        if (state.taxRate.isNotBlank() && taxRate == null) {
            _uiState.value = state.copy(errorMessage = "Invalid tax rate")
            return
        }
        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        val category = Category(
            id = categoryId ?: UUID.randomUUID().toString(),
            name = state.name,
            taxRate = taxRate,
            displayOrder = state.displayOrder.toIntOrNull() ?: 0
        )
        viewModelScope.launch {
            val result = if (state.isEditMode) productRepository.updateCategory(category)
            else productRepository.saveCategory(category)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }
}
