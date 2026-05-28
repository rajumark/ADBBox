package com.adbstudio.desktop.navigation

enum class NavigationItem(
    val displayName: String,
    val category: String = "Navigation",
) {
    Devices("Devices"),
    Apps("Apps"),
    Battery("Battery"),
    DebugInfo("Debug Info"),
    Settings("Settings"),
    UiInspector("UI Inspector"),
    Calendar("Calendar"),
    Contacts("Contacts"),
}
