package com.adbstudio.desktop.feature.settings.presentation

import com.adbstudio.desktop.feature.settings.data.SettingsCategory
import com.adbstudio.desktop.feature.settings.data.SettingsItem

sealed interface SettingsEvent {
    data class SelectCategory(val category: SettingsCategory) : SettingsEvent
    data class SetSearchQuery(val query: String) : SettingsEvent
    data class OpenSetting(val item: SettingsItem) : SettingsEvent
    data object DismissMessage : SettingsEvent
}
