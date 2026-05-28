package com.adbstudio.desktop.feature.settings.data

data class SettingsCategory(
    val id: String,
    val displayName: String,
    val items: List<SettingsItem>,
)

data class SettingsItem(
    val id: String,
    val displayName: String,
    val intent: String,
)
