package com.alphadragon.pos.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.Customer
import com.alphadragon.pos.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CustomersUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CustomersUiState> = combine(
        customerRepository.observeAll(),
        _searchQuery,
        _errorMessage
    ) { all, query, err ->
        val q = query.trim()
        val filtered = if (q.isBlank()) {
            all
        } else {
            all.filter { c ->
                c.name.contains(q, ignoreCase = true) ||
                    c.phone.contains(q, ignoreCase = true) ||
                    (c.email?.contains(q, ignoreCase = true) == true)
            }
        }
        CustomersUiState(
            customers = filtered,
            searchQuery = query,
            errorMessage = err
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CustomersUiState()
    )

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun createCustomer(name: String, phone: String, email: String?, note: String?) {
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
                onSuccess = { _errorMessage.value = null },
                onFailure = { _errorMessage.value = it.message ?: "Could not save customer" }
            )
        }
    }

    fun updateCustomer(customer: Customer, name: String, phone: String, email: String?, note: String?) {
        viewModelScope.launch {
            val updated = customer.copy(
                name = name.trim(),
                phone = phone.trim(),
                email = email?.trim()?.takeIf { it.isNotBlank() },
                note = note?.trim()?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )
            customerRepository.update(updated).fold(
                onSuccess = { _errorMessage.value = null },
                onFailure = { _errorMessage.value = it.message ?: "Could not update customer" }
            )
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            customerRepository.delete(id).fold(
                onSuccess = { _errorMessage.value = null },
                onFailure = { _errorMessage.value = it.message ?: "Could not delete customer" }
            )
        }
    }
}
