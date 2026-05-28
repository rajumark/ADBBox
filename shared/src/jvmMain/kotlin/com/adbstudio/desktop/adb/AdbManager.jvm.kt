package com.adbstudio.desktop.adb

import com.adbstudio.desktop.adb.model.base.AdbCommand
import com.adbstudio.desktop.core.error.AppError
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.platform.getAppCacheDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

actual class AdbManager {
    actual val adbPath: String
    actual val isReady: Boolean

    /** Default timeout for ADB commands. */
    private val defaultTimeoutMs: Long = 15_000

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

    actual suspend fun <T> run(command: AdbCommand<T>): AppResult<T> = withContext(Dispatchers.IO) {
        if (!isReady || adbPath.isBlank()) {
            return@withContext AppResult.Error(AppError.AdbNotReady(adbPath))
        }
        try {
            withTimeout(defaultTimeoutMs) {
                val process = ProcessBuilder(listOf(adbPath) + command.toCliArgs())
                    .redirectErrorStream(true)
                    .start()

                try {
                    val output = process.inputStream.bufferedReader().readText()
                    try {
                        process.waitFor()
                    } catch (_: Exception) {
                    }
                    currentCoroutineContext().ensureActive()
                    AppResult.Success(command.parse(output))
                } finally {
                    process.destroyForcibly()
                }
            }
        } catch (t: TimeoutCancellationException) {
            AppResult.Error(
                AppError.AdbCommandFailed(
                    command = command.id,
                    details = "Command timed out after ${defaultTimeoutMs}ms",
                ),
            )
        } catch (t: Throwable) {
            AppResult.Error(
                AppError.AdbCommandFailed(
                    command = command.id,
                    details = t.message ?: t.toString(),
                ),
            )
        }
    }

    actual suspend fun runShell(serial: String?, command: String): AppResult<String> = withContext(Dispatchers.IO) {
        if (!isReady || adbPath.isBlank()) {
            return@withContext AppResult.Error(AppError.AdbNotReady(adbPath))
        }
        try {
            withTimeout(defaultTimeoutMs) {
                val args = buildList {
                    if (!serial.isNullOrBlank()) {
                        add("-s")
                        add(serial)
                    }
                    add("shell")
                    add(command)
                }
                val process = ProcessBuilder(listOf(adbPath) + args)
                    .redirectErrorStream(true)
                    .start()

                try {
                    val output = process.inputStream.bufferedReader().readText()
                    try {
                        process.waitFor()
                    } catch (_: Exception) {
                    }
                    currentCoroutineContext().ensureActive()
                    AppResult.Success(output)
                } finally {
                    process.destroyForcibly()
                }
            }
        } catch (t: TimeoutCancellationException) {
            AppResult.Error(
                AppError.AdbCommandFailed(
                    command = "shell",
                    details = "Command timed out after ${defaultTimeoutMs}ms",
                ),
            )
        } catch (t: Throwable) {
            AppResult.Error(
                AppError.AdbCommandFailed(
                    command = "shell",
                    details = t.message ?: t.toString(),
                ),
            )
        }
    }

    actual suspend fun pullFile(serial: String?, remotePath: String): AppResult<ByteArray> = withContext(Dispatchers.IO) {
        if (!isReady || adbPath.isBlank()) {
            return@withContext AppResult.Error(AppError.AdbNotReady(adbPath))
        }
        val cacheDir = File(getAppCacheDir())
        val tempFile = File(cacheDir, "pull_${UUID.randomUUID()}.tmp")
        // File transfers may take longer — 60s timeout
        val pullTimeoutMs: Long = 60_000
        try {
            withTimeout(pullTimeoutMs) {
                val args = buildList {
                    if (!serial.isNullOrBlank()) {
                        add("-s")
                        add(serial)
                    }
                    add("pull")
                    add(remotePath)
                    add(tempFile.absolutePath)
                }
                val process = ProcessBuilder(listOf(adbPath) + args)
                    .redirectErrorStream(true)
                    .start()

                try {
                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        return@withTimeout AppResult.Error(
                            AppError.AdbCommandFailed(
                                command = "pull",
                                details = "Exit code $exitCode: $output",
                            ),
                        )
                    }
                    if (!tempFile.exists()) {
                        return@withTimeout AppResult.Error(
                            AppError.AdbCommandFailed(
                                command = "pull",
                                details = "Pulled file not found at ${tempFile.absolutePath}",
                            ),
                        )
                    }
                    currentCoroutineContext().ensureActive()
                    val bytes = tempFile.readBytes()
                    AppResult.Success(bytes)
                } finally {
                    process.destroyForcibly()
                }
            }
        } catch (t: TimeoutCancellationException) {
            AppResult.Error(
                AppError.AdbCommandFailed(
                    command = "pull",
                    details = "Pull timed out after ${pullTimeoutMs}ms",
                ),
            )
        } catch (t: Throwable) {
            AppResult.Error(
                AppError.AdbCommandFailed(
                    command = "pull",
                    details = t.message ?: t.toString(),
                ),
            )
        } finally {
            tempFile.delete()
        }
    }

    private fun runAdb(args: List<String>): String {
        val process = ProcessBuilder(listOf(adbPath) + args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        try {
            process.waitFor()
        } catch (_: Exception) {
        }
        return output
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
