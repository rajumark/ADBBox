package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class GrantPermissionCommand(
    override val serial: String?,
    val packageName: String,
    val permission: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.pm.grant"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "pm", "grant", packageName, permission))
    }

    override fun parse(output: String): Unit = Unit
}
