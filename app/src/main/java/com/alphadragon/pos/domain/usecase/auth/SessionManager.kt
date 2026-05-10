package com.alphadragon.pos.domain.usecase.auth

import com.alphadragon.pos.domain.model.Session
import com.alphadragon.pos.domain.repository.AppConfigRepository
import com.alphadragon.core.common.AppConfig
import com.alphadragon.core.common.ConfigKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session state. Never written to disk.
 * Expiration is checked on every POS action.
 */
@Singleton
class SessionManager @Inject constructor(
    private val appConfigRepository: AppConfigRepository
) {
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun isLoggedIn(): Boolean = _session.value != null && !isExpired()

    fun login(session: Session) {
        _session.value = session
    }

    fun logout() {
        _session.value = null
    }

    fun touchActivity() {
        _session.value = _session.value?.copy(lastActivityAt = System.currentTimeMillis())
    }

    fun isExpired(): Boolean {
        val s = _session.value ?: return true
        val timeoutMs = getSessionTimeoutMs()
        return System.currentTimeMillis() - s.lastActivityAt > timeoutMs
    }

    fun shouldRequirePinOnReturn(backgroundedAt: Long, bgTimeoutMinutes: Int = AppConfig.DEFAULT_BG_LOCK_TIMEOUT_MINUTES): Boolean {
        val bgTimeoutMs = bgTimeoutMinutes * 60_000L
        return System.currentTimeMillis() - backgroundedAt > bgTimeoutMs
    }

    private fun getSessionTimeoutMs(): Long {
        // Defaults to 8 hours — actual value loaded at use time from config
        return AppConfig.DEFAULT_SESSION_TIMEOUT_MINUTES * 60_000L
    }
}
