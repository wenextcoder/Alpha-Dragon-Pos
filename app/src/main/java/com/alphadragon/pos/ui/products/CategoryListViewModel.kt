package com.alphadragon.pos.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryListUiState(
    val categories: List<Category> = emptyList(),
    val deleteConfirmCategoryId: String? = null
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    val uiState: StateFlow<CategoryListUiState> = productRepository
        .observeTopLevelCategories()
        .map { CategoryListUiState(categories = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryListUiState())

    private val _deleteConfirmId = MutableStateFlow<String?>(null)

    val uiStateWithDelete: StateFlow<CategoryListUiState> = combine(
        productRepository.observeTopLevelCategories(),
        _deleteConfirmId
    ) { cats, deleteId ->
        CategoryListUiState(categories = cats, deleteConfirmCategoryId = deleteId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryListUiState())

    fun requestDeleteConfirm(id: String) { _deleteConfirmId.value = id }
    fun cancelDelete() { _deleteConfirmId.value = null }
    fun confirmDelete() {
        val id = _deleteConfirmId.value ?: return
        _deleteConfirmId.value = null
        viewModelScope.launch { productRepository.deactivateCategory(id) }
    }
}
