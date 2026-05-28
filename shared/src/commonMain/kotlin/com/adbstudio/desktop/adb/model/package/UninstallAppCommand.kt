package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class UninstallAppCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<Unit> {
    override val id: String = "uninstall"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("uninstall", packageName))
    }

    override fun parse(output: String): Unit = Unit
}
