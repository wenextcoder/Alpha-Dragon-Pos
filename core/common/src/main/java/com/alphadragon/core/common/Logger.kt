package com.alphadragon.core.common

import android.util.Log

/**
 * Release-safe logger. ENABLE_LOGGING is false in staging and release build types,
 * so R8 eliminates all log calls from production APKs.
 */
object Logger {
    private const val TAG = "AlphaDragon"

    fun d(message: String, tag: String = TAG) {
        if (BuildConfig.ENABLE_LOGGING) Log.d(tag, message)
    }

    fun i(message: String, tag: String = TAG) {
        if (BuildConfig.ENABLE_LOGGING) Log.i(tag, message)
    }

    fun w(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (BuildConfig.ENABLE_LOGGING) Log.w(tag, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (BuildConfig.ENABLE_LOGGING) Log.e(tag, message, throwable)
    }
}
