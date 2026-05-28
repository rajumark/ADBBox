package com.adbstudio.desktop.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

actual class AdbManager {
    actual val adbPath: String
    actual val isReady: Boolean

    init {
        val os = detectOs()
        val appDir = getAppDataDir(os)
        val adbFile = resolveAdb(os, appDir)
        if (adbFile != null) {
            grantPermissions(adbFile)
        }
        adbPath = adbFile?.absolutePath ?: ""
        isReady = adbFile?.exists() == true && adbFile.canExecute()
    }

    actual suspend fun listDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        if (!isReady || adbPath.isBlank()) return@withContext emptyList()

        val process = ProcessBuilder(listOf(adbPath, "devices"))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        // best-effort cleanup; ProcessBuilder usually completes quickly for this command
        try {
            process.waitFor()
        } catch (_: Exception) {
        }

        parseAdbDevicesOutput(output)
    }

    private fun parseAdbDevicesOutput(output: String): List<AdbDevice> {
        // Example:
        // List of devices attached
        // emulator-5554	device
        // R58M1234ABC	unauthorized
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("List of devices attached") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 3)
                val serial = parts.getOrNull(0)?.trim().orEmpty()
                val state = parts.getOrNull(1)?.trim().orEmpty()
                if (serial.isBlank() || state.isBlank()) null else AdbDevice(serial = serial, state = state)
            }
            .toList()
    }

    private fun resolveAdb(os: String, appDir: File): File? {
        return findAdbOnPath()
            ?: findAdbInSdk()
            ?: findAdbInAppDir(os, appDir)
    }

    private fun findAdbOnPath(): File? {
        return try {
            val command = if (detectOs() == "windows") listOf("where", "adb")
            else listOf("which", "adb")
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val path = process.inputStream.bufferedReader().readText().trim()
            if (path.isNotEmpty()) File(path) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun findAdbInSdk(): File? {
        val os = detectOs()
        val binaryName = adbBinaryName(os)
        val home = System.getProperty("user.home")
        val candidates = listOf(
            File(home, "Library/Android/sdk/platform-tools/$binaryName"),
            File(home, "Android/Sdk/platform-tools/$binaryName"),
            File(home, "android-sdk/platform-tools/$binaryName"),
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
    }

    private fun findAdbInAppDir(os: String, appDir: File): File? {
        val binaryName = adbBinaryName(os)
        val destDir = File(appDir, "platform-tools")
        val adbFile = File(destDir, binaryName)

        if (adbFile.exists() && adbFile.canExecute()) {
            return adbFile
        }

        return extractAdb(os, destDir, adbFile)
    }

    private fun extractAdb(os: String, destDir: File, adbFile: File): File? {
        return try {
            destDir.mkdirs()
            val zipName = "platform-tools-$os.zip"
            val zipStream = AdbManager::class.java.classLoader
                ?.getResourceAsStream("adb/$zipName")
                ?: return null

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

            if (adbFile.exists()) adbFile else null
        } catch (_: Exception) {
            null
        }
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
