package com.adbstudio.desktop.device

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class DeviceManager(private val adbPath: String) {

    private val _devices = mutableStateListOf<DeviceInfo>()
    val devices: List<DeviceInfo> get() = _devices

    var selectedDeviceId: String? by mutableStateOf(null)
        private set

    val selectedDevice: DeviceInfo?
        get() = devices.find { it.id == selectedDeviceId }

    fun refresh() {
        val parsed = fetchDevices()
        _devices.clear()
        _devices.addAll(parsed)

        val current = selectedDeviceId
        selectedDeviceId = when {
            current != null && parsed.any { it.id == current } -> current
            parsed.isNotEmpty() -> parsed.first().id
            else -> null
        }
    }

    fun selectDevice(id: String) {
        if (_devices.any { it.id == id }) {
            selectedDeviceId = id
        }
    }

    private fun fetchDevices(): List<DeviceInfo> {
        return try {
            val process = ProcessBuilder(adbPath, "devices")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            parseOutput(output)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseOutput(output: String): List<DeviceInfo> {
        return output.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { it.trim().split("\t", limit = 2) }
            .filter { it.size == 2 && it[1] == "device" }
            .map { DeviceInfo(id = it[0].trim(), state = it[1].trim()) }
            .toList()
    }
}
