package com.adbstudio.desktop.adb.model.inspector

import com.adbstudio.desktop.adb.model.base.AdbCommand

/**
 * Fetches the current focused activity via dumpsys window.
 * No shell pipes are used; filtering happens in [parse] for cross-platform safety.
 */
data class GetCurrentActivityCommand(
    override val serial: String?,
) : AdbCommand<String> {
    override val id: String = "shell.dumpsys.window.current"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "dumpsys", "window"))
    }

    override fun parse(output: String): String {
        val lines = output.lines()

        // Priority 1: mCurrentFocus or mFocusedApp
        val focusLine = lines.firstOrNull {
            it.contains("mCurrentFocus") || it.contains("mFocusedApp")
        }
        if (focusLine != null) {
            val activity = extractActivityName(focusLine)
            if (activity.isNotBlank()) return activity
        }

        // Priority 2: mResumedActivity / topResumedActivity from activity manager
        val resumedLine = lines.firstOrNull {
            it.contains("mResumedActivity") || it.contains("topResumedActivity")
        }
        if (resumedLine != null) {
            val activity = extractActivityName(resumedLine)
            if (activity.isNotBlank()) return activity
        }

        return "Unable to detect current activity"
    }

    private fun extractActivityName(line: String): String {
        val regex = Regex("([a-zA-Z0-9._]+)/([a-zA-Z0-9._]+)")
        val match = regex.find(line)
        return if (match != null) {
            val packageName = match.groupValues[1]
            val activityName = match.groupValues[2]
            if (activityName.startsWith(".")) "$packageName$activityName" else "$packageName/$activityName"
        } else {
            ""
        }
    }
}
