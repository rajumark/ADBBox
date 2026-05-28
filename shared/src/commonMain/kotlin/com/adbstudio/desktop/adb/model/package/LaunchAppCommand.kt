package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class LaunchAppCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.monkey.launch"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1"))
    }

    override fun parse(output: String): Unit = Unit
}
