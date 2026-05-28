package com.adbstudio.desktop.adb.model.battery

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class DumpsysBatteryCommand(
    override val serial: String?,
) : AdbCommand<BatteryInfo> {
    override val id: String = "shell.dumpsys.battery"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "dumpsys", "battery"))
    }

    override fun parse(output: String): BatteryInfo {
        val raw = output.trim()
        val entries = raw
            .lineSequence()
            .map { it.trim() }
            .filter { it.contains(":") }
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) return@mapNotNull null
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .distinctBy { it.first }
            .toList()
        return BatteryInfo(raw = raw, entries = entries)
    }
}

