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

    /**
     * Runs: `adb [-s serial] shell pm list packages`
     */
    suspend fun listPackages(serial: String?): List<String>

    /**
     * Runs: `adb [-s serial] shell dumpsys battery`
     */
    suspend fun dumpsysBattery(serial: String?): String
}
