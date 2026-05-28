package com.adbstudio.desktop.feature.systemdetails.presentation

sealed interface SystemDetailsEvent {
    data object Refresh : SystemDetailsEvent
    data class SetSearchQuery(val query: String) : SystemDetailsEvent
    data class SetTab(val tab: DetailsTab) : SystemDetailsEvent
}

enum class DetailsTab(val command: String, val label: String, val prefix: String) {
    Features("pm list features", "Features", "feature:"),
    Permissions("pm list permissions", "Permissions", "permission:"),
    Libraries("pm list libraries", "Libraries", "library:"),
}
