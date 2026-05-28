package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class DumpsysPackageCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<String> {
    override val id: String = "shell.dumpsys.package"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "dumpsys", "package", packageName))
    }

    override fun parse(output: String): String = output
}
