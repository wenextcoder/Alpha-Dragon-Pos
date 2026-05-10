package com.alphadragon.pos.domain.usecase.auth

import com.alphadragon.pos.domain.model.Admin
import com.alphadragon.pos.domain.repository.AdminRepository
import com.alphadragon.pos.domain.repository.AuditLogRepository
import com.alphadragon.pos.security.DeviceFingerprint
import com.alphadragon.pos.security.PinHasher
import com.alphadragon.pos.domain.model.AuditLogEntry
import com.alphadragon.core.common.AppConfig
import java.util.UUID
import javax.inject.Inject

/**
 * First-launch only. Creates the Admin account, stores bcrypt PIN hash,
 * generates device fingerprint, and marks setup complete.
 */
class SetupAdminUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val auditLogRepository: AuditLogRepository,
    private val pinHasher: PinHasher,
    private val deviceFingerprint: DeviceFingerprint
) {
    sealed class SetupError {
        object InvalidPin : SetupError()
        object WeakPin : SetupError()
        object PinMismatch : SetupError()
        object InvalidUsername : SetupError()
        object EmptyShopName : SetupError()
        data class StorageFailure(val cause: Throwable) : SetupError()
    }

    suspend operator fun invoke(
        shopName: String,
        adminName: String,
        username: String,
        pin: String,
        pinConfirm: String
    ): Result<Unit> {
        // Validation
        if (shopName.isBlank()) return Result.failure(Exception("Empty shop name"))
        if (username.isBlank() || username.contains(' ')) return Result.failure(Exception("Invalid username"))
        if (pin.length != AppConfig.PIN_LENGTH || !pin.all { it.isDigit() })
            return Result.failure(Exception("PIN must be exactly 6 digits"))
        if (pin != pinConfirm) return Result.failure(Exception("PINs do not match"))

        val pinHash = pinHasher.hash(pin)
        val now = System.currentTimeMillis()

        val admin = Admin(
            id = UUID.randomUUID().toString(),
            username = username.lowercase(),
            displayName = adminName,
            pinHash = pinHash,
            createdAt = now
        )

        return adminRepository.createAdmin(admin, shopName).also { result ->
            if (result.isSuccess) {
                deviceFingerprint.generateAndStore()
                auditLogRepository.log(
                    AuditLogEntry(
                        id = UUID.randomUUID().toString(),
                        action = "ADMIN_SETUP",
                        detail = "{\"username\":\"${admin.username}\"}",
                        timestamp = now
                    )
                )
            }
        }
    }
}
