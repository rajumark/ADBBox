package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class RevokePermissionCommand(
    override val serial: String?,
    val packageName: String,
    val permission: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.pm.revoke"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "pm", "revoke", packageName, permission))
    }

    override fun parse(output: String): Unit = Unit
}
