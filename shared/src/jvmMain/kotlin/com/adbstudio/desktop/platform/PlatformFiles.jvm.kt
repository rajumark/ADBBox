package com.adbstudio.desktop.platform

import java.io.File

actual fun getAppCacheDir(): String {
    val os = detectOs()
    val home = System.getProperty("user.home")
    val cacheDir = when (os) {
        "macos" -> File(home, "Library/Caches/ADBStudio")
        "linux" -> File(home, ".cache/ADBStudio")
        "windows" -> {
            val localAppData = System.getenv("LOCALAPPDATA")
            val base = if (localAppData != null) {
                File(localAppData, "Temp")
            } else {
                File(home, "AppData/Local/Temp")
            }
            File(base, "ADBStudio")
        }
        else -> File(home, ".cache/ADBStudio")
    }
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    return cacheDir.absolutePath
}

private fun detectOs(): String {
    val name = System.getProperty("os.name").lowercase()
    return when {
        name.contains("mac") -> "macos"
        name.contains("linux") || name.contains("nix") || name.contains("nux") -> "linux"
        name.contains("windows") -> "windows"
        else -> "linux"
    }
}
