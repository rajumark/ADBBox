package com.adbstudio.desktop.adb.model.settings

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class OpenAndroidSettingsCommand(
    override val serial: String?,
    val intent: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.am.open_settings"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "am", "start", "-a", intent))
    }

    override fun parse(output: String): Unit = Unit
}
