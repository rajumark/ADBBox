package com.adbstudio.desktop.adb.model.battery

data class BatteryInfo(
    val raw: String,
    val entries: List<Pair<String, String>>,
)

