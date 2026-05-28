package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class PackagePathCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<List<String>> {
    override val id: String = "shell.pm.path"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "pm", "path", packageName))
    }

    override fun parse(output: String): List<String> {
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .toList()
    }
}
