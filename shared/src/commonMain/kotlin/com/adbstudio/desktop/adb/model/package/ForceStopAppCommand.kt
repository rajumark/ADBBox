package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class ForceStopAppCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.am.force_stop"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "am", "force-stop", packageName))
    }

    override fun parse(output: String): Unit = Unit
}
