package com.adbstudio.desktop.adb.model.base

/**
 * Categories of ADB commands (§3, §14).
 */
enum class CommandCategory(val displayName: String) {
    DEVICE("Device"),
    PACKAGE("Package"),
    LOGCAT("Logcat"),
    SHELL("Shell"),
    WIRELESS("Wireless"),
    DUMPSYS("Dumpsys"),
    SETTINGS("Settings"),
    CONTENT("Content"),
    INPUT("Input"),
    WINDOW_MANAGER("Window Manager"),
    ACTIVITY_MANAGER("Activity Manager"),
    SCREEN_CAPTURE("Screen Capture"),
    FILE_TRANSFER("File Transfer"),
    BATTERY("Battery"),
    MEDIA("Media"),
    CALENDAR("Calendar"),
    CONTACTS("Contacts"),
    MESSAGES("Messages"),
    LIFECYCLE("Lifecycle"),
    DEBUG("Debug"),
}

/**
 * Metadata for a typed ADB command (§3, §14).
 *
 * Powers command palette, docs, warnings, filtering, AI tooling.
 * Each command registers its metadata at startup in a [CommandRegistry].
 */
data class CommandMetadata(
    /** Unique command identifier, e.g., "shell.dumpsys.battery" */
    val id: String,
    /** Command category for grouping in palette/docs */
    val category: CommandCategory,
    /** Human-readable name */
    val displayName: String,
    /** Description of what the command does */
    val description: String = "",
    /** true = may cause data loss or system changes (e.g., uninstall, wipe) */
    val dangerous: Boolean = false,
    /** true = requires root access */
    val requiresRoot: Boolean = false,
    /** Minimum Android API level required, null = no restriction */
    val minApi: Int? = null,
    /** true = command produces streaming output (e.g., logcat) */
    val supportsStreaming: Boolean = false,
)
