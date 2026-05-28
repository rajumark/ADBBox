package com.adbstudio.desktop.navigation

enum class NavigationItem(
    val displayName: String,
    val category: String = "Navigation",
) {
    Devices("Devices"),
    Apps("Apps"),
    DebugInfo("Debug Info"),
    Settings("Settings"),
    UiInspector("UI Inspector"),
}
