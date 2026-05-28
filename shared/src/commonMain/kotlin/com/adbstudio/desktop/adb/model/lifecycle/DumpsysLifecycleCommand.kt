package com.adbstudio.desktop.adb.model.lifecycle

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class DumpsysLifecycleCommand(
    override val serial: String?,
) : AdbCommand<List<LogEntry>> {
    override val id: String = "shell.dumpsys.usagestats"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "dumpsys", "usagestats"))
    }

    override fun parse(output: String): List<LogEntry> {
        return output.lineSequence()
            .map { it.trim() }
            .mapNotNull { parseLogEntry(it) }
            .toList()
            .reversed()
    }

    private fun parseLogEntry(logString: String): LogEntry? {
        val entries = logString.split(" ").map { it.trim() }
        val parsedValues = mutableMapOf<String, String?>()

        for (entry in entries) {
            val parts = entry.split("=")
            if (parts.size == 2) {
                parsedValues[parts[0]] = parts[1].removeSurrounding("\"")
            }
        }

        if (parsedValues["time"] == null || parsedValues["type"] == null) return null

        return LogEntry(
            time = parsedValues["time"],
            type = parsedValues["type"],
            packageName = parsedValues["package"],
            className = parsedValues["class"],
            instanceId = parsedValues["instanceId"],
            taskRootPackage = parsedValues["taskRootPackage"],
            taskRootClass = parsedValues["taskRootClass"],
            flags = parsedValues["flags"]
        )
    }
}
