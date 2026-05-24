package com.adbstudio.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbstudio.desktop.commander.CommanderAction
import com.adbstudio.desktop.commander.CommanderDialog
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.device.DeviceInfo
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
    selectedDevice: DeviceInfo?,
    commanderOpen: Boolean,
    onCommanderDismiss: () -> Unit,
    onCommanderAction: (CommanderAction) -> Unit,
    commanderRegistry: CommanderRegistry,
) {
    AdbStudioTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (navigationItem) {
                    NavigationItem.Apps -> AppsScreen(adbManager = adbManager)
                    NavigationItem.DebugInfo -> DebugInfoScreen(
                        adbManager = adbManager,
                        selectedDevice = selectedDevice,
                    )
                    NavigationItem.Settings -> SettingsScreen()
                    NavigationItem.UiInspector -> UiInspectorScreen()
                }
            }

            AnimatedVisibility(
                visible = commanderOpen,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CommanderDialog(
                    registry = commanderRegistry,
                    onDismiss = onCommanderDismiss,
                    onActionSelected = onCommanderAction,
                )
            }
        }
    }
}
