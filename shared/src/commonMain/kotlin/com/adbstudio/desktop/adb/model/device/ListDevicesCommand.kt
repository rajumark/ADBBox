package com.adbstudio.desktop.adb.model.device

import com.adbstudio.desktop.adb.AdbDevice
import com.adbstudio.desktop.adb.model.base.AdbCommand

data class ListDevicesCommand(
    override val serial: String? = null,
) : AdbCommand<List<AdbDevice>> {
    override val id: String = "devices"

    override fun toCliArgs(): List<String> = listOf("devices")

    override fun parse(output: String): List<AdbDevice> {
        // Example:
        // List of devices attached
        // emulator-5554    device
        // R58M1234ABC      unauthorized
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("List of devices attached") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 3)
                val s = parts.getOrNull(0)?.trim().orEmpty()
                val state = parts.getOrNull(1)?.trim().orEmpty()
                if (s.isBlank() || state.isBlank()) null else AdbDevice(serial = s, state = state)
            }
            .toList()
    }
}

