package com.adbstudio.desktop.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppIconService(private val adbPath: String, private val appDataDir: String) {

    private var lastDeviceId: String? = null
    private val serverDexRemote = "/data/local/tmp/adbstudio-server.dex"
    private val serverIconDir = "/data/local/tmp/adbstudio/icons"

    private fun deviceDir(deviceId: String): File = File(appDataDir, "icons/$deviceId")
    private fun metaFile(deviceId: String): File = File(deviceDir(deviceId), "metadata.txt")
    private fun iconFile(deviceId: String, pkg: String): File = File(deviceDir(deviceId), "$pkg.png")

    fun loadCache(deviceId: String): AppCache {
        val dir = deviceDir(deviceId)
        if (!dir.isDirectory) return AppCache(emptyMap(), emptyMap())

        val labels = mutableMapOf<String, String>()
        val icons = mutableMapOf<String, String?>()
        val meta = readMeta(deviceId)

        for ((pkg, entry) in meta) {
            labels[pkg] = entry.label
            val icon = iconFile(deviceId, pkg)
            icons[pkg] = if (icon.isFile) icon.absolutePath else null
        }

        return AppCache(labels, icons)
    }

    suspend fun fetchAndCache(deviceId: String, packageNames: List<String>) {
        if (packageNames.isEmpty()) return
        withContext(Dispatchers.IO) {
            val meta = readMeta(deviceId)
            val needFetch = packageNames.filter { pkg ->
                val cached = meta[pkg]
                cached == null || cached.apkSize == 0L
            }
            if (needFetch.isEmpty()) return@withContext

            ensureServer(deviceId)
            val results = runExtractor(deviceId, needFetch)

            for (entry in results) {
                meta[entry.packageName] = CacheEntry(entry.label, entry.apkSize)
                if (entry.iconCachePath.isNotEmpty()) {
                    pullIcon(deviceId, entry.iconCachePath, entry.packageName)
                }
            }

            writeMeta(deviceId, meta)
        }
    }

    private fun readMeta(deviceId: String): MutableMap<String, CacheEntry> {
        val file = metaFile(deviceId)
        if (!file.isFile) return mutableMapOf()
        val map = mutableMapOf<String, CacheEntry>()
        for (line in file.readLines()) {
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val pkg = line.substring(0, eq)
            val rest = line.substring(eq + 1)
            val pipe = rest.lastIndexOf('|')
            if (pipe <= 0) continue
            val label = rest.substring(0, pipe)
            val size = rest.substring(pipe + 1).toLongOrNull() ?: 0L
            map[pkg] = CacheEntry(label, size)
        }
        return map
    }

    private fun writeMeta(deviceId: String, meta: Map<String, CacheEntry>) {
        val dir = deviceDir(deviceId)
        dir.mkdirs()
        val lines = meta.map { (pkg, entry) -> "$pkg=${entry.label}|${entry.apkSize}" }
        metaFile(deviceId).writeText(lines.joinToString("\n"))
    }

    private fun ensureServer(deviceId: String) {
        if (lastDeviceId == deviceId) return
        lastDeviceId = null

        val dexBytes = AppIconService::class.java.classLoader
            ?.getResourceAsStream("adbstudio-server.dex")
            ?.readBytes()
            ?: throw IllegalStateException("adbstudio-server.dex not found in resources")

        val tempFile = File(appDataDir, ".adbstudio-server-tmp.dex")
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
            if (exitCode != 0) throw RuntimeException("Failed to push server dex: $output")
        } finally {
            tempFile.delete()
        }

        lastDeviceId = deviceId
    }

    private fun runExtractor(deviceId: String, packageNames: List<String>): List<ExtractorEntry> {
        val results = mutableListOf<ExtractorEntry>()
        for (chunk in packageNames.chunked(30)) {
            val args = mutableListOf(
                adbPath, "-s", deviceId, "shell",
                "CLASSPATH=$serverDexRemote",
                "app_process", "/system/bin", "com.adbstudio.IconExtractor",
            )
            args.addAll(chunk)
            try {
                val process = ProcessBuilder(args).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                results.addAll(parseExtractorOutput(output))
            } catch (_: Exception) { }
        }
        return results
    }

    private fun pullIcon(deviceId: String, remotePath: String, packageName: String) {
        val local = iconFile(deviceId, packageName)
        if (local.exists()) return
        local.parentFile.mkdirs()
        try {
            val process = ProcessBuilder(
                adbPath, "-s", deviceId, "exec-out", "cat", remotePath,
            ).start()
            val bytes = process.inputStream.readBytes()
            process.waitFor()
            if (bytes.isNotEmpty()) local.writeBytes(bytes)
        } catch (_: Exception) { }
    }

    internal fun parseExtractorOutput(output: String): List<ExtractorEntry> {
        val result = mutableListOf<ExtractorEntry>()
        try {
            val pkgIdx = output.indexOf("\"packageInfos\":[")
            if (pkgIdx < 0) return result
            val arrayStart = output.indexOf('[', pkgIdx)
            val arrayEnd = output.lastIndexOf(']')
            if (arrayStart < 0 || arrayEnd < 0 || arrayEnd <= arrayStart) return result
            val content = output.substring(arrayStart + 1, arrayEnd)
            var depth = 0
            var start = -1
            for (i in content.indices) {
                when (content[i]) {
                    '{' -> { if (depth == 0) start = i; depth++ }
                    '}' -> {
                        depth--
                        if (depth == 0 && start >= 0) {
                            parseEntry(content.substring(start, i + 1))?.let { result.add(it) }
                            start = -1
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        return result
    }

    internal fun parseEntry(json: String): ExtractorEntry? {
        return try {
            val pkg = extractString(json, "packageName") ?: return null
            ExtractorEntry(
                packageName = pkg,
                label = extractString(json, "label") ?: pkg,
                iconCachePath = extractString(json, "iconCachePath") ?: "",
                apkSize = extractLong(json, "apkSize") ?: 0,
            )
        } catch (_: Exception) { null }
    }

    internal fun extractString(json: String, key: String): String? {
        val search = "\"$key\":\""
        val start = json.indexOf(search)
        if (start < 0) return null
        val vs = start + search.length
        val end = json.indexOf('"', vs)
        if (end < 0) return null
        return json.substring(vs, end).replace("\\/", "/")
    }

    internal fun extractLong(json: String, key: String): Long? {
        val search = "\"$key\":"
        val start = json.indexOf(search)
        if (start < 0) return null
        val vs = start + search.length
        val comma = json.indexOf(',', vs)
        val brace = json.indexOf('}', vs)
        val end = when {
            comma >= 0 && brace >= 0 -> minOf(comma, brace)
            comma >= 0 -> comma
            brace >= 0 -> brace
            else -> return null
        }
        return json.substring(vs, end).trim().toLongOrNull()
    }

    fun clearCache(deviceId: String) {
        deviceDir(deviceId).deleteRecursively()
    }

    fun clearAllCaches() {
        File(appDataDir, "icons").deleteRecursively()
    }
}

data class AppCache(
    val labels: Map<String, String>,
    val icons: Map<String, String?>,
)

internal data class CacheEntry(val label: String, val apkSize: Long)

internal data class ExtractorEntry(
    val packageName: String,
    val label: String,
    val iconCachePath: String,
    val apkSize: Long,
)
