package com.adbstudio.desktop.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppIconService(private val adbPath: String, private val appDataDir: String) {

    private val localIconDir: File = File(appDataDir, "icons")
    private var lastDeviceId: String? = null
    private val serverDexRemote = "/data/local/tmp/adbstudio-server.dex"
    private val iconCacheDir = "/data/local/tmp/adbstudio/icons"

    init {
        localIconDir.mkdirs()
    }

    suspend fun getPackageInfos(
        deviceId: String,
        packageNames: List<String>,
    ): List<PackageInfoWithIcon> = withContext(Dispatchers.IO) {
        if (packageNames.isEmpty()) return@withContext emptyList()

        ensureServer(deviceId)

        val result = runExtractor(deviceId, packageNames)

        result.map { info ->
            val localPath = if (info.iconCachePath.isNotEmpty()) {
                val cacheKey = "${info.packageName}.${info.apkSize}"
                val localFile = File(localIconDir, "$cacheKey.png")
                if (!localFile.exists()) {
                    pullIcon(deviceId, info.iconCachePath, localFile)
                }
                localFile.absolutePath
            } else null

            PackageInfoWithIcon(
                packageName = info.packageName,
                label = info.label,
                iconLocalPath = localPath,
                versionName = info.versionName,
                enabled = info.enabled,
                isSystem = info.system,
            )
        }
    }

    private fun ensureServer(deviceId: String) {
        if (lastDeviceId == deviceId) return
        lastDeviceId = null

        val dexBytes = AppIconService::class.java.classLoader
            ?.getResourceAsStream("adbstudio-server.dex")
            ?.readBytes()
            ?: throw IllegalStateException("adbstudio-server.dex not found in resources")

        val tempFile = File(localIconDir.parentFile ?: File("/tmp"), ".adbstudio-server-tmp.dex")
        try {
            tempFile.outputStream().use { it.write(dexBytes) }

            val process = ProcessBuilder(
                adbPath, "-s", deviceId, "push",
                tempFile.absolutePath, serverDexRemote,
            )
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("Failed to push server dex: $output")
            }
        } finally {
            tempFile.delete()
        }

        lastDeviceId = deviceId
    }

    data class ExtractorEntry(
        val packageName: String,
        val label: String,
        val iconCachePath: String,
        val apkPath: String,
        val apkSize: Long,
        val enabled: Boolean,
        val system: Boolean,
        val versionName: String,
    )

    private fun runExtractor(
        deviceId: String,
        packageNames: List<String>,
    ): List<ExtractorEntry> {
        val batchSize = 30
        val results = mutableListOf<ExtractorEntry>()

        for (chunk in packageNames.chunked(batchSize)) {
            val args = mutableListOf(
                adbPath, "-s", deviceId, "shell",
                "CLASSPATH=$serverDexRemote",
                "app_process", "/system/bin", "com.adbstudio.IconExtractor",
            )
            args.addAll(chunk)

            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val entries = parseExtractorOutput(output)
            results.addAll(entries)
        }

        return results
    }

    internal fun parseExtractorOutput(output: String): List<ExtractorEntry> {
        val result = mutableListOf<ExtractorEntry>()
        try {
            val packageInfosStart = output.indexOf("\"packageInfos\":[")
            if (packageInfosStart == -1) return result
            val arrayStart = output.indexOf('[', packageInfosStart)
            val arrayEnd = output.lastIndexOf(']')
            if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) return result

            val arrayContent = output.substring(arrayStart + 1, arrayEnd)
            var depth = 0
            var currentStart = -1
            for (i in arrayContent.indices) {
                when (arrayContent[i]) {
                    '{' -> {
                        if (depth == 0) currentStart = i
                        depth++
                    }
                    '}' -> {
                        depth--
                        if (depth == 0 && currentStart != -1) {
                            val objStr = arrayContent.substring(currentStart, i + 1)
                            val entry = parseEntry(objStr)
                            if (entry != null) result.add(entry)
                            currentStart = -1
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to parse extractor output: $output")
        }
        return result
    }

    internal fun parseEntry(json: String): ExtractorEntry? {
        return try {
            val pkgName = extractJsonString(json, "packageName") ?: return null
            val label = extractJsonString(json, "label") ?: pkgName
            val iconCachePath = extractJsonString(json, "iconCachePath") ?: ""
            val apkPath = extractJsonString(json, "apkPath") ?: ""
            val apkSize = extractJsonLong(json, "apkSize") ?: 0
            val enabled = extractJsonBoolean(json, "enabled") ?: true
            val system = extractJsonBoolean(json, "system") ?: false
            val versionName = extractJsonString(json, "versionName") ?: ""

            ExtractorEntry(
                packageName = pkgName,
                label = label,
                iconCachePath = iconCachePath,
                apkPath = apkPath,
                apkSize = apkSize,
                enabled = enabled,
                system = system,
                versionName = versionName,
            )
        } catch (_: Exception) { null }
    }

    internal fun extractJsonString(json: String, key: String): String? {
        val searchKey = "\"$key\":\""
        val start = json.indexOf(searchKey)
        if (start == -1) return null
        val valueStart = start + searchKey.length
        val end = json.indexOf('"', valueStart)
        if (end == -1) return null
        return json.substring(valueStart, end).replace("\\/", "/")
    }

    internal fun extractJsonLong(json: String, key: String): Long? {
        val searchKey = "\"$key\":"
        val start = json.indexOf(searchKey)
        if (start == -1) return null
        val valueStart = start + searchKey.length
        val end = json.indexOf(',', valueStart).let { if (it == -1) json.indexOf('}', valueStart) else it }
        if (end == -1) return null
        return json.substring(valueStart, end).trim().toLongOrNull()
    }

    internal fun extractJsonBoolean(json: String, key: String): Boolean? {
        val searchKey = "\"$key\":"
        val start = json.indexOf(searchKey)
        if (start == -1) return null
        val valueStart = start + searchKey.length
        return when {
            json.regionMatches(valueStart, "true", 0, 4) -> true
            json.regionMatches(valueStart, "false", 0, 5) -> false
            else -> null
        }
    }

    private fun pullIcon(deviceId: String, remotePath: String, localFile: File) {
        if (localFile.exists()) return
        try {
            val process = ProcessBuilder(
                adbPath, "-s", deviceId, "pull", remotePath,
                localFile.absolutePath,
            )
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (!localFile.exists()) {
                System.err.println("Warning: failed to pull icon $remotePath: $output")
            }
        } catch (e: Exception) {
            System.err.println("Warning: error pulling icon $remotePath: ${e.message}")
        }
    }

    fun resetForDevice() {
        lastDeviceId = null
    }

    fun clearIconCache() {
        localIconDir.listFiles()?.forEach { it.delete() }
    }
}

data class PackageInfoWithIcon(
    val packageName: String,
    val label: String,
    val iconLocalPath: String?,
    val versionName: String,
    val enabled: Boolean,
    val isSystem: Boolean,
) {
    fun toPackageInfo() = PackageInfo(
        packageName = packageName,
        label = label,
        iconLocalPath = iconLocalPath,
        versionName = versionName,
        enabled = enabled,
        isSystem = isSystem,
    )
}
