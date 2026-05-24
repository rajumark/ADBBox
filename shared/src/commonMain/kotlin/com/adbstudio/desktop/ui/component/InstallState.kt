package com.adbstudio.desktop.ui.component

sealed class InstallState {
    data object Idle : InstallState()
    data class Installing(val fileName: String) : InstallState()
    data class Success(val message: String) : InstallState()
    data class Error(val summary: String, val fullLog: String) : InstallState()
}
