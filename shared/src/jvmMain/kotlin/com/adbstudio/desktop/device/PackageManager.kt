package com.adbstudio.desktop.device

import androidx.compose.runtime.mutableStateListOf

class PackageManager(private val adbPath: String) {

    private val _packages = mutableStateListOf<PackageInfo>()
    val packages: List<PackageInfo> get() = _packages

    fun refresh(deviceId: String, filter: PackageFilter = PackageFilter.User) {
        val parsed = fetchPackages(deviceId, filter)
        if (parsed != _packages.toList()) {
            _packages.clear()
            _packages.addAll(parsed)
        }
    }

    fun mergeLabels(labels: Map<String, String>) {
        val updated = _packages.map { pkg ->
            val label = labels[pkg.packageName]
            if (label != null && label != pkg.label) pkg.copy(label = label) else pkg
        }
        if (updated != _packages.toList()) {
            _packages.clear()
            _packages.addAll(updated)
        }
    }

    fun mergeIcons(icons: Map<String, String?>) {
        val updated = _packages.map { pkg ->
            val iconPath = icons[pkg.packageName]
            if (iconPath != null && iconPath != pkg.iconLocalPath) pkg.copy(iconLocalPath = iconPath) else pkg
        }
        if (updated != _packages.toList()) {
            _packages.clear()
            _packages.addAll(updated)
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
