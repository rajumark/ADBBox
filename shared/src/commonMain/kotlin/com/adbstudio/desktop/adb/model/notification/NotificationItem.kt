package com.adbstudio.desktop.adb.model.notification

data class NotificationItem(
    val id: String,
    val packageName: String,
    val channelId: String = "",
    val importance: String = "",
    val title: String = "",
    val text: String = "",
    val timestamp: String = "",
    val color: String = "",
    val flags: String = "",
    val visibility: String = "",
    val userId: String = "",
    val key: String = "",
)
