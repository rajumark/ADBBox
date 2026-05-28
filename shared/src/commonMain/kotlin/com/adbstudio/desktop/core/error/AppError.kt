package com.adbstudio.desktop.core.error

sealed interface AppError {
    data class AdbNotReady(val adbPath: String) : AppError
    data class AdbCommandFailed(val command: String, val details: String) : AppError
    data class Unknown(val message: String) : AppError
}

fun AppError.toUserMessage(): String = when (this) {
    is AppError.AdbNotReady -> "ADB is not ready"
    is AppError.AdbCommandFailed -> "ADB command failed: $command"
    is AppError.Unknown -> message
}

