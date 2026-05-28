package com.adbstudio.desktop.feature.apps.model

enum class AppType(val displayName: String, val flag: String?) {
    ALL("All", null),
    USER("User", "-3"),
    SYSTEM("System", "-s"),
    ENABLED("Enabled", "-e"),
    DISABLED("Disabled", "-d"),
    UNINSTALLED("Uninstalled", "-u"),
}
