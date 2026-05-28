package com.adbstudio.desktop.feature.deviceproperties.presentation

sealed interface DevicePropertiesEvent {
    data object Refresh : DevicePropertiesEvent
    data class SetSearchQuery(val query: String) : DevicePropertiesEvent
}
