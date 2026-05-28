package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class EnableAppCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.pm.enable"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "pm", "enable", packageName))
    }

    override fun parse(output: String): Unit = Unit
}
