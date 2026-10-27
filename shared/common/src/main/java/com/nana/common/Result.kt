package com.nana.common

/**
 * Result monad for error handling without exceptions.
 * All core modules use this for fallible operations.
 */
sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val error: Error) : Result<Nothing>

    data class Error(
        val code: String,
        val message: String,
        val cause: Throwable? = null,
        val recoverable: Boolean = true
    ) {
        companion object {
            fun unknown(cause: Throwable? = null) = Error("UNKNOWN", "Unknown error", cause)
            fun notFound(what: String) = Error("NOT_FOUND", "$what not found")
            fun permissionDenied(what: String) = Error("PERMISSION_DENIED", "Permission denied: $what", recoverable = false)
            fun timeout(operation: String) = Error("TIMEOUT", "$operation timed out")
            fun modelError(details: String) = Error("MODEL_ERROR", "Model error: $details", recoverable = false)
            fun invalidInput(details: String) = Error("INVALID_INPUT", "Invalid input: $details", recoverable = false)
        }
    }

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    fun onSuccess(action: (T) -> Unit): Result<T> = also { if (it is Success) action(it.value) }
    fun onFailure(action: (Error) -> Unit): Result<T> = also { if (it is Failure) action(it.error) }

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw RuntimeException(error.message, error.cause)
    }

    fun getOrDefault(default: T): T = when (this) {
        is Success -> value
        is Failure -> default
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
}
