package com.adbstudio.desktop.feature.apps.presentation

import com.adbstudio.desktop.feature.apps.model.AppType

sealed interface AppsEvent {
    data object Refresh : AppsEvent
    data class SetFilter(val appType: AppType) : AppsEvent
    data class SetSearchQuery(val query: String) : AppsEvent
    data class SelectPackage(val packageName: String?) : AppsEvent
    data class TogglePin(val packageName: String) : AppsEvent
    data class SetDetailsTab(val tab: DetailsTab) : AppsEvent
    data object LaunchApp : AppsEvent
    data object ForceStopApp : AppsEvent
    data object RestartApp : AppsEvent
    data object UninstallApp : AppsEvent
    data object ClearData : AppsEvent
    data object EnableApp : AppsEvent
    data object DisableApp : AppsEvent
    data object OpenAppSettings : AppsEvent
    data object CopyPackageName : AppsEvent
    data object ViewOnPlayStore : AppsEvent
    data object DownloadApk : AppsEvent
    data object RefreshDetails : AppsEvent
    data object DismissActionMessage : AppsEvent
}

