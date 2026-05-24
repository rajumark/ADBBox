package com.adbstudio.desktop.adb

expect class AdbManager() {
    val adbPath: String
    val isReady: Boolean
    val appDataDir: String
}
