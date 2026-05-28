package com.adbstudio.desktop.core.events

import com.adbstudio.desktop.adb.AdbDevice

sealed interface AppEvent {
    data class DeviceListUpdated(val devices: List<AdbDevice>) : AppEvent
    data class SelectedDeviceChanged(val serial: String?) : AppEvent
}

