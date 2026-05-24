package com.adbstudio.desktop.navigation

enum class NavigationItem(
    val displayName: String,
    val category: String = "Navigation",
) {
    Apps("Apps"),
    Settings("Settings"),
    UiInspector("UI Inspector"),
}
