package com.adbstudio.desktop.feature.battery.presentation

import com.adbstudio.desktop.adb.model.battery.BatteryInfo

data class BatteryUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val batteryInfo: BatteryInfo? = null,
)

