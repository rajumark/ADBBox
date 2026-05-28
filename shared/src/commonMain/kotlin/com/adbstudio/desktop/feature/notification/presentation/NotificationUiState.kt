package com.adbstudio.desktop.feature.notification.presentation

import com.adbstudio.desktop.adb.model.notification.NotificationItem

data class NotificationUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val notifications: List<NotificationItem> = emptyList(),
) {
    val filteredNotifications: List<NotificationItem>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return notifications
            return notifications.filter { item ->
                item.packageName.lowercase().contains(query) ||
                    item.title.lowercase().contains(query) ||
                    item.text.lowercase().contains(query) ||
                    item.channelId.lowercase().contains(query) ||
                    item.importance.lowercase().contains(query)
            }
        }
}
