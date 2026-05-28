package com.adbstudio.desktop.core.result

import com.adbstudio.desktop.core.error.AppError

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

