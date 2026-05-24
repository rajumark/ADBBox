package com.adbstudio.desktop.device

enum class PackageFilter(
    val displayName: String,
) {
    User("User Apps"),
    Debug("Debug Apps"),
    System("System Apps"),
    Disabled("Disabled Apps"),
    All("All Apps"),
}
