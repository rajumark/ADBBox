package com.adbstudio.desktop.feature.notification.presentation

sealed interface NotificationEvent {
    data object Refresh : NotificationEvent
    data class SetSearchQuery(val query: String) : NotificationEvent
}
