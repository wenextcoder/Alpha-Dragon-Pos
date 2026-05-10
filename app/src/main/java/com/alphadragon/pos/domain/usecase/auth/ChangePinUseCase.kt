package com.alphadragon.pos.domain.usecase.auth

import com.alphadragon.pos.domain.model.AuditLogEntry
import com.alphadragon.pos.domain.repository.AdminRepository
import com.alphadragon.pos.domain.repository.AuditLogRepository
import com.alphadragon.pos.security.PinHasher
import com.alphadragon.core.common.AppConfig
import java.util.UUID
import javax.inject.Inject

class ChangePinUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val auditLogRepository: AuditLogRepository,
    private val pinHasher: PinHasher
) {
    suspend operator fun invoke(
        currentPin: String,
        newPin: String,
        newPinConfirm: String
    ): Result<Unit> {
        val admin = adminRepository.getAdmin()
            ?: return Result.failure(IllegalStateException("No admin found"))

        if (!pinHasher.verify(currentPin, admin.pinHash))
            return Result.failure(IllegalArgumentException("Current PIN is incorrect"))

        if (newPin.length != AppConfig.PIN_LENGTH || !newPin.all { it.isDigit() })
            return Result.failure(IllegalArgumentException("PIN must be exactly 6 digits"))

        if (newPin != newPinConfirm)
            return Result.failure(IllegalArgumentException("New PIN does not match confirmation"))

        val newHash = pinHasher.hash(newPin)
        return adminRepository.updatePinHash(admin.id, newHash).also { result ->
            if (result.isSuccess) {
                auditLogRepository.log(
                    AuditLogEntry(
                        id = UUID.randomUUID().toString(),
                        action = "PIN_CHANGED",
                        detail = null,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
