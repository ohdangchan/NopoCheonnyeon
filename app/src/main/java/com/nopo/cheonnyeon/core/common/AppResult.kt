package com.nopo.cheonnyeon.core.common

/** 성공 값 또는 [AppError]. 예외 대신 이것을 반환한다. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    val errorOrNull: AppError? get() = (this as? Failure)?.error
    val valueOrNull: T? get() = (this as? Success)?.value
}

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)
fun AppError.asFailure(): AppResult<Nothing> = AppResult.Failure(this)
