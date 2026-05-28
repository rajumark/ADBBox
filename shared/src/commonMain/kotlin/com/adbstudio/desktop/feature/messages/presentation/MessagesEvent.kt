package com.adbstudio.desktop.feature.messages.presentation

sealed interface MessagesEvent {
    data object Refresh : MessagesEvent
    data class SetSearchQuery(val query: String) : MessagesEvent
    data object ToggleOriginal : MessagesEvent
}
