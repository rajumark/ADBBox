package com.adbstudio.desktop.feature.processes.presentation

sealed interface ProcessesEvent {
    data object Refresh : ProcessesEvent
    data class SetSearchQuery(val query: String) : ProcessesEvent
    data object ToggleShowAll : ProcessesEvent
}
