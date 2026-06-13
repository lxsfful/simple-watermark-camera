package com.lx.simplewatermarkcamera.core

import java.util.concurrent.CancellationException

sealed interface OperationResult<out T> {
    data class Success<T>(val value: T) : OperationResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : OperationResult<Nothing>
}

inline fun <T> runOperation(message: String, block: () -> T): OperationResult<T> = try {
    OperationResult.Success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    OperationResult.Failure(message, error)
}
