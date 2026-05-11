package com.alphadragon.pos.ui.shop

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.core.common.ConfigKeys
import com.alphadragon.pos.data.storage.ProductImageStorage
import com.alphadragon.pos.domain.repository.AppConfigRepository
import com.alphadragon.pos.ui.currency.DEFAULT_CURRENCY_CODE
import com.alphadragon.pos.ui.currency.isSupportedCurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopDetailsUiState(
    val shopName: String = "",
    val shopAddress: String = "",
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val logoPath: String? = null,
    val isLogoLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ShopDetailsViewModel @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    private val imageStorage: ProductImageStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopDetailsUiState())
    val uiState: StateFlow<ShopDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = ShopDetailsUiState(
                shopName = appConfigRepository.get(ConfigKeys.SHOP_NAME) ?: "",
                shopAddress = appConfigRepository.get(ConfigKeys.SHOP_ADDRESS) ?: "",
                currencyCode = appConfigRepository.get(ConfigKeys.SHOP_CURRENCY)
                    ?.takeIf { isSupportedCurrencyCode(it) } ?: DEFAULT_CURRENCY_CODE,
                logoPath = appConfigRepository.get(ConfigKeys.SHOP_LOGO_PATH),
            )
        }
        viewModelScope.launch {
            appConfigRepository.observe(ConfigKeys.SHOP_CURRENCY).collect { c ->
                _uiState.value = _uiState.value.copy(
                    currencyCode = c?.takeIf { isSupportedCurrencyCode(it) } ?: DEFAULT_CURRENCY_CODE
                )
            }
        }
    }

    fun updateShopName(value: String) {
        _uiState.value = _uiState.value.copy(shopName = value, saveMessage = null, errorMessage = null)
    }

    fun updateShopAddress(value: String) {
        _uiState.value = _uiState.value.copy(shopAddress = value, saveMessage = null, errorMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(saveMessage = null, errorMessage = null)
    }

    fun save() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            _uiState.value = snapshot.copy(isSaving = true, errorMessage = null, saveMessage = null)
            val nameResult = appConfigRepository.set(ConfigKeys.SHOP_NAME, snapshot.shopName.trim())
            val addressResult = appConfigRepository.set(ConfigKeys.SHOP_ADDRESS, snapshot.shopAddress.trim())
            when {
                nameResult.isFailure -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = nameResult.exceptionOrNull()?.message ?: "Could not save name"
                    )
                }
                addressResult.isFailure -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = addressResult.exceptionOrNull()?.message ?: "Could not save address"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveMessage = "Saved")
                }
            }
        }
    }

    fun onLogoPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLogoLoading = true, errorMessage = null)
            val oldPath = _uiState.value.logoPath
            val newPath = imageStorage.copyFromUri(uri, replacingPath = oldPath)
            if (newPath == null) {
                _uiState.value = _uiState.value.copy(isLogoLoading = false, errorMessage = "Could not save logo")
                return@launch
            }
            appConfigRepository.set(ConfigKeys.SHOP_LOGO_PATH, newPath).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLogoLoading = false, logoPath = newPath)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLogoLoading = false,
                        errorMessage = e.message ?: "Could not store logo reference"
                    )
                }
            )
        }
    }

    fun removeLogo() {
        val path = _uiState.value.logoPath ?: return
        viewModelScope.launch {
            imageStorage.deleteFile(path)
            appConfigRepository.delete(ConfigKeys.SHOP_LOGO_PATH).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(logoPath = null) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Could not clear logo")
                }
            )
        }
    }
}
