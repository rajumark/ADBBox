package com.adbstudio.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.AdbStudioTheme
import com.adbstudio.desktop.theme.ThemeMode
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.ui.screen.AppsScreen
import com.adbstudio.desktop.ui.screen.DebugInfoScreen
import com.adbstudio.desktop.ui.screen.SettingsScreen
import com.adbstudio.desktop.ui.screen.UiInspectorScreen

@Composable
fun App(
    themeMode: ThemeMode,
    navigationItem: NavigationItem,
    adbManager: AdbManager,
) {
    AdbStudioTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (navigationItem) {
                NavigationItem.Apps -> AppsScreen(adbManager = adbManager)
                NavigationItem.DebugInfo -> DebugInfoScreen(adbManager = adbManager)
                NavigationItem.Settings -> SettingsScreen()
                NavigationItem.UiInspector -> UiInspectorScreen()
            }
        }
    }
}