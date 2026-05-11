package com.alphadragon.pos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.core.common.ConfigKeys
import com.alphadragon.pos.domain.repository.AppConfigRepository
import com.alphadragon.pos.ui.currency.DEFAULT_CURRENCY_CODE
import com.alphadragon.pos.ui.currency.isSupportedCurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appConfigRepository.observe(ConfigKeys.SHOP_CURRENCY).collect { currency ->
                _uiState.value = _uiState.value.copy(
                    currencyCode = currency?.takeIf(::isSupportedCurrencyCode) ?: DEFAULT_CURRENCY_CODE,
                    errorMessage = null
                )
            }
        }
    }

    fun updateCurrency(currencyCode: String) {
        if (!isSupportedCurrencyCode(currencyCode)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unsupported currency")
            return
        }

        viewModelScope.launch {
            appConfigRepository.set(ConfigKeys.SHOP_CURRENCY, currencyCode).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(errorMessage = null) },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Could not save currency"
                    )
                }
            )
        }
    }
}
