package com.alphadragon.pos.security

import android.content.Context
import com.scottyab.rootbeer.RootBeer
import com.alphadragon.core.common.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RootBeer-backed root detection.
 * A rooted device shows a warning to Admin but does NOT block operation —
 * some legitimate POS devices have modified firmware.
 * The detection result is logged to the audit trail.
 */
@Singleton
class RootDetection @Inject constructor() {

    fun isRooted(context: Context): Boolean =
        runCatching {
            RootBeer(context).isRooted
        }.getOrElse { throwable ->
            Logger.w("Root detection error — assuming not rooted", throwable, "RootDetection")
            false
        }
}
