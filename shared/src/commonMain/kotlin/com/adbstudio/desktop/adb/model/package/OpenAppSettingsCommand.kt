package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class OpenAppSettingsCommand(
    override val serial: String?,
    val packageName: String,
) : AdbCommand<Unit> {
    override val id: String = "shell.am.app_settings"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf(
            "shell", "am", "start", "-a",
            "android.settings.APPLICATION_DETAILS_SETTINGS",
            "-d", "package:$packageName",
        ))
    }

    override fun parse(output: String): Unit = Unit
}
