package com.adbstudio.desktop.adb

expect class AdbManager() {
    val adbPath: String
    val isReady: Boolean

    /**
     * Runs `adb devices` and returns parsed devices.
     *
     * Note: the window UI should call this from a coroutine (IO work).
     */
    suspend fun listDevices(): List<AdbDevice>
}
