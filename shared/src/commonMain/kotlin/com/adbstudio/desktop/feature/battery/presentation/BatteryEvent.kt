package com.adbstudio.desktop.feature.battery.presentation

sealed interface BatteryEvent {
    data object Refresh : BatteryEvent
}

