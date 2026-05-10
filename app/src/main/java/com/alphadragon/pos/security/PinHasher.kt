package com.alphadragon.pos.security

import com.alphadragon.core.common.AppConfig
import org.mindrot.jbcrypt.BCrypt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * bcrypt PIN hashing at cost factor 12.
 * Plain PINs are never logged, stored, or returned from this class.
 */
@Singleton
class PinHasher @Inject constructor() {

    fun hash(pin: String): String =
        BCrypt.hashpw(pin, BCrypt.gensalt(AppConfig.BCRYPT_COST))

    fun verify(pin: String, hash: String): Boolean =
        runCatching { BCrypt.checkpw(pin, hash) }.getOrDefault(false)

    companion object {
        /** Any 6-digit numeric PIN is accepted. */
        fun isPinAllowed(pin: String): Boolean =
            pin.length == AppConfig.PIN_LENGTH && pin.all { it.isDigit() }
    }
}
