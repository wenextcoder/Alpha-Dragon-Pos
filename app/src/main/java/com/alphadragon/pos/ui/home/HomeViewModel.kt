package com.alphadragon.pos.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.AuditLogEntry
import com.alphadragon.pos.domain.repository.AdminRepository
import com.alphadragon.pos.domain.repository.AuditLogRepository
import com.alphadragon.pos.domain.usecase.auth.SessionManager
import com.alphadragon.pos.security.DeviceFingerprint
import com.alphadragon.pos.security.RootDetection
import com.alphadragon.pos.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val sessionManager: SessionManager,
    private val deviceFingerprint: DeviceFingerprint,
    private val rootDetection: RootDetection,
    private val auditLogRepository: AuditLogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _startDestination = MutableStateFlow(NavRoutes.LOGIN)
    val startDestination: StateFlow<String> = _startDestination.asStateFlow()

    private val _startDestinationReady = MutableStateFlow(false)
    val startDestinationReady: StateFlow<Boolean> = _startDestinationReady.asStateFlow()

    private val _showRootWarning = MutableStateFlow(false)
    val showRootWarning: StateFlow<Boolean> = _showRootWarning.asStateFlow()

    private val _deviceMismatch = MutableStateFlow(false)
    val deviceMismatch: StateFlow<Boolean> = _deviceMismatch.asStateFlow()

    init {
        viewModelScope.launch {
            val isSetup = adminRepository.isSetupComplete()
            _startDestination.value = if (isSetup) NavRoutes.LOGIN else NavRoutes.SETUP
            _startDestinationReady.value = true

            // Verify device fingerprint on every launch (only after setup is complete)
            if (isSetup) {
                val result = deviceFingerprint.verify()
                if (result is DeviceFingerprint.FingerprintResult.Mismatch) {
                    _deviceMismatch.value = true
                    auditLogRepository.log(
                        AuditLogEntry(
                            id = UUID.randomUUID().toString(),
                            action = "DEVICE_FP_MISMATCH",
                            detail = "{\"stored\":\"${result.stored}\",\"current\":\"${result.current}\"}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            _showRootWarning.value = rootDetection.isRooted(context)
        }
    }

    fun dismissRootWarning() { _showRootWarning.value = false }

    fun acknowledgeFingerprintMismatch() { _deviceMismatch.value = false }
}
