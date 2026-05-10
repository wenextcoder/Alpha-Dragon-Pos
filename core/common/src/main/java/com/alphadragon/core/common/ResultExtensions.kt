package com.alphadragon.core.common

/**
 * Convenience extensions for Kotlin's built-in Result<T>.
 * All fallible operations in this project return Result<T>.
 */

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(onSuccess = { transform(it) }, onFailure = { Result.failure(it) })

inline fun <T> Result<T>.onFailureLog(tag: String = "AlphaDragon"): Result<T> =
    onFailure { Logger.e(it.message ?: "Unknown error", it, tag) }

fun <T> Result<T>.getOrThrowDomain(): T =
    getOrElse { throw it }
