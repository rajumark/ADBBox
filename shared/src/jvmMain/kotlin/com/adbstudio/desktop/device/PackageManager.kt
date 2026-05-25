package com.adbstudio.desktop.device

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PackageManager(
    private val adbPath: String,
    private val iconService: AppIconService? = null,
) {

    private val _packages = mutableStateListOf<PackageInfo>()
    val packages: List<PackageInfo> get() = _packages

    private var lastPackageNames = setOf<String>()
    private val enrichedCache = mutableMapOf<String, PackageInfoWithIcon>()

    suspend fun refresh(deviceId: String, filter: PackageFilter = PackageFilter.User) {
        val parsed = fetchPackages(deviceId, filter)
        if (parsed != _packages.toList()) {
            _packages.clear()
            _packages.addAll(parsed)
        }
    }

    suspend fun refreshWithIcons(
        deviceId: String,
        filter: PackageFilter = PackageFilter.User,
        iconService: AppIconService,
    ) {
        val parsed = fetchPackages(deviceId, filter)

        val currentNames = parsed.map { it.packageName }.toSet()
        val newNames = currentNames - lastPackageNames
        val removedNames = lastPackageNames - currentNames

        if (removedNames.isNotEmpty()) {
            removedNames.forEach { enrichedCache.remove(it) }
        }

        if (newNames.isNotEmpty()) {
            try {
                val enriched = iconService.getPackageInfos(deviceId, newNames.toList())
                enriched.forEach { enrichedCache[it.packageName] = it }
            } catch (e: Exception) {
                System.err.println("Failed to fetch icons: ${e.message}")
            }
        }

        lastPackageNames = currentNames

        val merged = parsed.map { pkg ->
            val cached = enrichedCache[pkg.packageName]
            if (cached != null) {
                PackageInfo(
                    packageName = pkg.packageName,
                    label = cached.label,
                    iconLocalPath = cached.iconLocalPath,
                    versionName = cached.versionName,
                    enabled = cached.enabled,
                    isSystem = cached.isSystem,
                )
            } else {
                pkg
            }
        }

        if (merged != _packages.toList()) {
            _packages.clear()
            _packages.addAll(merged)
        }
    }

    private fun fetchPackages(deviceId: String, filter: PackageFilter): List<PackageInfo> {
        val args = mutableListOf(adbPath, "-s", deviceId, "shell", "pm", "list", "packages")
        val flag = when (filter) {
            PackageFilter.User -> "-3"
            PackageFilter.System -> "-s"
            PackageFilter.Disabled -> "-d"
            PackageFilter.All -> null
            PackageFilter.Debug -> null
        }
        flag?.let { args.add(it) }
        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            parseOutput(output)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseOutput(output: String): List<PackageInfo> {
        return output.lineSequence()
            .filter { it.startsWith("package:") }
            .map { PackageInfo(packageName = it.removePrefix("package:").trim()) }
            .sortedBy { it.packageName }
            .toList()
    }
}
