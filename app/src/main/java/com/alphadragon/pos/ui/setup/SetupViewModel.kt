package com.alphadragon.pos.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.core.common.ConfigKeys
import com.alphadragon.pos.domain.repository.AppConfigRepository
import com.alphadragon.pos.domain.usecase.auth.LoginResult
import com.alphadragon.pos.domain.usecase.auth.LoginUseCase
import com.alphadragon.pos.domain.usecase.auth.SessionManager
import com.alphadragon.pos.domain.usecase.auth.SetupAdminUseCase
import com.alphadragon.pos.ui.currency.DEFAULT_CURRENCY_CODE
import com.alphadragon.pos.ui.currency.isSupportedCurrencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val shopName: String = "",
    val adminName: String = "",
    val username: String = "",
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val pin: String = "",
    val pinConfirm: String = ""
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val setupAdminUseCase: SetupAdminUseCase,
    private val loginUseCase: LoginUseCase,
    private val sessionManager: SessionManager,
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun updateShopName(value: String) { _uiState.value = _uiState.value.copy(shopName = value, errorMessage = null) }
    fun updateAdminName(value: String) { _uiState.value = _uiState.value.copy(adminName = value, errorMessage = null) }
    fun updateUsername(value: String) { _uiState.value = _uiState.value.copy(username = value.filter { !it.isWhitespace() }, errorMessage = null) }
    fun updateCurrency(value: String) {
        if (isSupportedCurrencyCode(value)) {
            _uiState.value = _uiState.value.copy(currencyCode = value, errorMessage = null)
        }
    }
    fun updatePin(value: String) { _uiState.value = _uiState.value.copy(pin = value, errorMessage = null) }
    fun updatePinConfirm(value: String) { _uiState.value = _uiState.value.copy(pinConfirm = value, errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            setupAdminUseCase(
                shopName = state.shopName,
                adminName = state.adminName,
                username = state.username,
                pin = state.pin,
                pinConfirm = state.pinConfirm
            ).fold(
                onSuccess = {
                    appConfigRepository.set(ConfigKeys.SHOP_CURRENCY, state.currencyCode).fold(
                        onSuccess = { autoLogin(state.pin) },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Could not save currency"
                            )
                        }
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message ?: "Setup failed")
                }
            )
        }
    }

    private suspend fun autoLogin(pin: String) {
        when (val result = loginUseCase(pin)) {
            is LoginResult.Success -> {
                sessionManager.login(result.session)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }
            else -> {
                // Should never happen right after creating the account, but handle gracefully
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }
        }
    }
}
