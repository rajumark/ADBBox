package com.adbstudio.desktop.feature.devicesettings.presentation

sealed interface DeviceSettingsEvent {
    data object Refresh : DeviceSettingsEvent
    data class SetSearchQuery(val query: String) : DeviceSettingsEvent
    data class SetTab(val tab: SettingsTab) : DeviceSettingsEvent
}

enum class SettingsTab(val command: String, val label: String) {
    All("settings list global", "All"),
    Global("settings list global", "Global"),
    System("settings list system", "System"),
    Secure("settings list secure", "Secure"),
}
