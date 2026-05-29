package com.adbstudio.desktop.adb.model.processes

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class PsCommand(
    override val serial: String?,
    val showAll: Boolean = true,
) : AdbCommand<ProcessInfo> {
    override val id: String = "shell.ps"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(
            listOf("shell", "ps") + if (showAll) listOf("-A") else emptyList()
        )
    }

    override fun parse(output: String): ProcessInfo {
        val raw = output.trim()
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return ProcessInfo(raw = raw, entries = emptyList())

        val headerLine = lines.first()
        val headers = headerLine.split(Regex("\\s+")).filter { it.isNotBlank() }

        val dataLines = lines.drop(1)
        val entries = dataLines.mapNotNull { line ->
            parsePsLine(line, headers)
        }

        return ProcessInfo(raw = raw, entries = entries)
    }

    private fun parsePsLine(line: String, headers: List<String>): Map<String, String>? {
        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < headers.size) return null

        val nameIndex = headers.indexOf("NAME").takeIf { it >= 0 }
            ?: headers.indexOf("name").takeIf { it >= 0 }
            ?: (headers.size - 1)

        val result = mutableMapOf<String, String>()
        for (i in 0 until nameIndex) {
            if (i < headers.size && i < parts.size) {
                result[headers[i]] = parts[i]
            }
        }

        val name = parts.drop(nameIndex).joinToString(" ")
        if (nameIndex < headers.size) {
            result[headers[nameIndex]] = name
        }

        return if (result.isNotEmpty()) result else null
    }

    companion object {
        val columnSortOrder: List<String> = listOf(
            "USER", "PID", "PPID", "VSZ", "RSS", "WCHAN", "ADDR", "S", "NAME",
        )
    }
}
