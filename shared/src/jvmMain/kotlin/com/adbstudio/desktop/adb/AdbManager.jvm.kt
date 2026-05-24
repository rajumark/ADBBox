package com.adbstudio.desktop.adb

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

actual class AdbManager {
    actual val adbPath: String
    actual val isReady: Boolean

    init {
        val os = detectOs()
        val appDir = getAppDataDir(os)
        val adbFile = extractAdb(os, appDir)
        grantPermissions(adbFile)
        adbPath = adbFile.absolutePath
        isReady = adbFile.exists() && adbFile.canExecute()
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

    private fun getAppDataDir(os: String): File {
        val home = System.getProperty("user.home")
        return when (os) {
            "macos" -> File(home, "Library/Application Support/ADBStudio")
            "linux" -> File(home, ".local/share/ADBStudio")
            "windows" -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                if (localAppData != null) File(localAppData, "ADBStudio")
                else File(home, "AppData/Local/ADBStudio")
            }
            else -> File(home, ".ADBStudio")
        }
    }

    private fun adbBinaryName(os: String): String = when (os) {
        "windows" -> "adb.exe"
        else -> "adb"
    }

    private fun extractAdb(os: String, appDir: File): File {
        val binaryName = adbBinaryName(os)
        val destDir = File(appDir, "platform-tools")
        val adbFile = File(destDir, binaryName)

        if (adbFile.exists()) {
            return adbFile
        }

        destDir.mkdirs()

        val zipName = "platform-tools-$os.zip"
        val zipStream = Thread.currentThread().contextClassLoader
            .getResourceAsStream("adb/$zipName")
            ?: throw IllegalStateException("ADB zip not found in resources: $zipName")

        ZipInputStream(zipStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return adbFile
    }

    private fun grantPermissions(adbFile: File) {
        try {
            adbFile.setExecutable(true)
        } catch (_: SecurityException) {
        }
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", adbFile.absolutePath))
        } catch (_: Exception) {
        }
    }
}
