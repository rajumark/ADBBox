package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class ListPackagesCommand(
    override val serial: String?,
    val filter: String? = null,
) : AdbCommand<List<String>> {
    override val id: String = "shell.pm.list_packages"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "pm", "list", "packages"))
        if (!filter.isNullOrBlank()) {
            add(filter)
        }
    }

    override fun parse(output: String): List<String> {
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                if (line.startsWith("package:")) line.removePrefix("package:").trim() else null
            }
            .toList()
    }
}

