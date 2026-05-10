package com.alphadragon.pos.domain.usecase.auth

import com.alphadragon.pos.domain.model.Admin
import com.alphadragon.pos.domain.model.AuditLogEntry
import com.alphadragon.pos.domain.model.Session
import com.alphadragon.pos.domain.repository.AdminRepository
import com.alphadragon.pos.domain.repository.AuditLogRepository
import com.alphadragon.pos.security.PinHasher
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoginResult {
    data class Success(val session: Session) : LoginResult()
    object InvalidCredentials : LoginResult()
    data class LockedOut(val remainingSeconds: Long) : LoginResult()
}

/** Manages login attempt tracking in memory — never persisted. */
@Singleton
class LoginAttemptTracker @Inject constructor() {
    private var failureCount = 0
    private var lockoutUntil = 0L
    private val maxAttempts = 5
    private val lockoutMs = 30_000L

    fun isLockedOut(): Boolean = System.currentTimeMillis() < lockoutUntil

    fun remainingLockoutSeconds(): Long =
        maxOf(0L, (lockoutUntil - System.currentTimeMillis()) / 1000)

    fun recordFailure() {
        failureCount++
        if (failureCount >= maxAttempts) {
            lockoutUntil = System.currentTimeMillis() + lockoutMs
            failureCount = 0
        }
    }

    fun recordSuccess() { failureCount = 0; lockoutUntil = 0 }
}

class LoginUseCase @Inject constructor(
    private val adminRepository: AdminRepository,
    private val auditLogRepository: AuditLogRepository,
    private val pinHasher: PinHasher,
    private val attemptTracker: LoginAttemptTracker
) {
    suspend operator fun invoke(pin: String): LoginResult {
        if (attemptTracker.isLockedOut()) {
            return LoginResult.LockedOut(attemptTracker.remainingLockoutSeconds())
        }

        val admin: Admin = adminRepository.getAdmin()
            ?: return LoginResult.InvalidCredentials

        val now = System.currentTimeMillis()

        if (!pinHasher.verify(pin, admin.pinHash)) {
            attemptTracker.recordFailure()
            auditLogRepository.log(
                AuditLogEntry(
                    id = UUID.randomUUID().toString(),
                    action = "LOGIN_FAILURE",
                    detail = "{\"attempts_remaining\":${5 - 0}}",
                    timestamp = now
                )
            )
            return if (attemptTracker.isLockedOut())
                LoginResult.LockedOut(attemptTracker.remainingLockoutSeconds())
            else
                LoginResult.InvalidCredentials
        }

        attemptTracker.recordSuccess()
        adminRepository.updateLastLogin(admin.id, now)
        auditLogRepository.log(
            AuditLogEntry(
                id = UUID.randomUUID().toString(),
                action = "LOGIN_SUCCESS",
                detail = "{\"username\":\"${admin.username}\"}",
                timestamp = now
            )
        )

        val session = Session(
            token = UUID.randomUUID().toString(),
            adminId = admin.id,
            createdAt = now,
            lastActivityAt = now
        )
        return LoginResult.Success(session)
    }
}
