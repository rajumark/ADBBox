package com.adbstudio.desktop.adb.model.processes

data class ProcessInfo(
    val raw: String,
    val entries: List<Map<String, String>>,
)
