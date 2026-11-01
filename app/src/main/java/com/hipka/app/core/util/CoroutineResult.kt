package com.hipka.app.core.util

import kotlinx.coroutines.CancellationException

inline fun <T> coroutineResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
