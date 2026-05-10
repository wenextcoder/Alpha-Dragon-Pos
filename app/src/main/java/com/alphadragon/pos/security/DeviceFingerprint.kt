package com.alphadragon.pos.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.alphadragon.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device fingerprint: SHA-256 of ANDROID_ID + build fingerprint + model + manufacturer.
 * Generated once at first launch (during admin setup), stored in hardware-backed
 * EncryptedSharedPreferences, and verified on every subsequent launch.
 */
@Singleton
class DeviceFingerprint @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val PREF_FILE = "device_fp_enc"
    private val PREF_KEY = "fp"

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun generateAndStore(): String {
        val fp = compute()
        prefs.edit().putString(PREF_KEY, fp).apply()
        Logger.d("Device fingerprint generated and stored", "DeviceFingerprint")
        return fp
    }

    fun verify(): FingerprintResult {
        val stored = runCatching { prefs.getString(PREF_KEY, null) }.getOrNull()
            ?: return FingerprintResult.NotFound
        val current = compute()
        return if (stored == current) FingerprintResult.Match
        else FingerprintResult.Mismatch(stored = stored, current = current)
    }

    fun getCurrentFingerprint(): String =
        runCatching { prefs.getString(PREF_KEY, null) }.getOrNull() ?: compute()

    @SuppressLint("HardwareIds")
    private fun compute(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        val buildFp = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val raw = "$androidId|$buildFp|$model|$manufacturer"
        return sha256(raw)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    sealed class FingerprintResult {
        object Match : FingerprintResult()
        object NotFound : FingerprintResult()
        data class Mismatch(val stored: String, val current: String) : FingerprintResult()
    }
}
