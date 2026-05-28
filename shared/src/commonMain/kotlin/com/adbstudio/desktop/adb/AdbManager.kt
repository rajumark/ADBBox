package com.adbstudio.desktop.adb

import com.adbstudio.desktop.adb.model.base.AdbCommand
import com.adbstudio.desktop.core.result.AppResult

expect class AdbManager() {
    val adbPath: String
    val isReady: Boolean

    /**
     * Runs a typed ADB command (see project_rules.md §3).
     */
    suspend fun <T> run(command: AdbCommand<T>): AppResult<T>

    /**
     * Runs an arbitrary shell command via `adb shell`.
     */
    suspend fun runShell(serial: String?, command: String): AppResult<String>

    /**
     * Pulls a file from the device to a temporary local location and returns its bytes.
     */
    suspend fun pullFile(serial: String?, remotePath: String): AppResult<ByteArray>
}
