package com.adbstudio.desktop.feature.lifecycle.presentation

import com.adbstudio.desktop.adb.model.lifecycle.LogEntry

data class LifecycleUiState(
    val selectedSerial: String? = null,
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
