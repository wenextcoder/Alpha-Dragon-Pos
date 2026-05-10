package com.alphadragon.pos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.usecase.auth.LoginResult
import com.alphadragon.pos.domain.usecase.auth.LoginUseCase
import com.alphadragon.pos.domain.usecase.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val pin: String = "",
    val errorMessage: String? = null,
    val lockedOutSeconds: Long = 0L
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updatePin(value: String) {
        _uiState.value = _uiState.value.copy(pin = value, errorMessage = null)
        if (value.length == 6) login(value)
    }

    fun login(pin: String = _uiState.value.pin) {
        val state = _uiState.value
        if (state.isLoading || state.lockedOutSeconds > 0) return
        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = loginUseCase(pin)) {
                is LoginResult.Success -> {
                    sessionManager.login(result.session)
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is LoginResult.InvalidCredentials -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pin = "",
                        errorMessage = "Incorrect PIN. Please try again."
                    )
                }
                is LoginResult.LockedOut -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pin = "",
                        lockedOutSeconds = result.remainingSeconds,
                        errorMessage = null
                    )
                    startLockoutCountdown(result.remainingSeconds)
                }
            }
        }
    }

    private fun startLockoutCountdown(seconds: Long) {
        viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.value = _uiState.value.copy(lockedOutSeconds = remaining)
            }
        }
    }
}
