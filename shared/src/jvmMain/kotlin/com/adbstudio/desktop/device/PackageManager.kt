package com.adbstudio.desktop.device

import androidx.compose.runtime.mutableStateListOf

class PackageManager(private val adbPath: String) {

    private val _packages = mutableStateListOf<PackageInfo>()
    val packages: List<PackageInfo> get() = _packages

    fun refresh(deviceId: String) {
        val parsed = fetchPackages(deviceId)
        if (parsed != _packages.toList()) {
            _packages.clear()
            _packages.addAll(parsed)
        }
    }

    private fun fetchPackages(deviceId: String): List<PackageInfo> {
        return try {
            val process = ProcessBuilder(
                adbPath, "-s", deviceId, "shell", "pm", "list", "packages",
            )
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
