package com.adbstudio.desktop.adb.model.`package`

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class GetForegroundAppCommand(
    override val serial: String?,
) : AdbCommand<String?> {
    override val id: String = "shell.dumpsys.activity.top"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "dumpsys", "activity", "activities"))
    }

    override fun parse(output: String): String? {
        val focusedLine = output.lines().firstOrNull {
            it.contains("mResumedActivity") || it.contains("mFocusedActivity")
        } ?: return null

        val regex = Regex("u0\\s+(\\S+)/")
        return regex.find(focusedLine)?.groupValues?.get(1)
    }
}
