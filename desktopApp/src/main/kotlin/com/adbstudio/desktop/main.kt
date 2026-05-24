package com.adbstudio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.commander.CommanderAction
import com.adbstudio.desktop.commander.CommanderHost
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.device.DeviceManager
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.ThemeMode
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

fun main() = application {
    val adbManager = remember { AdbManager() }
    val deviceManager = remember { DeviceManager(adbManager.adbPath) }
    var commanderOpen by remember { mutableStateOf(false) }
    var lastShiftTime by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val doubleShiftThresholdMs = 400.0

    LaunchedEffect(Unit) {
        while (true) {
            deviceManager.refresh()
            delay(3000)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ADBStudio",
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.ShiftLeft || event.key == Key.ShiftRight)
            ) {
                val now = TimeSource.Monotonic.markNow()
                val elapsed = (now - lastShiftTime).inWholeMilliseconds
                lastShiftTime = now
                if (elapsed < doubleShiftThresholdMs) {
                    commanderOpen = !commanderOpen
                }
                true
            } else {
                false
            }
        },
    ) {
        var themeMode by remember { mutableStateOf(ThemeMode.System) }
        var navigationItem by remember { mutableStateOf(NavigationItem.Apps) }
        val commanderRegistry = remember { CommanderRegistry() }

        LaunchedEffect(Unit) {
            NavigationItem.entries.forEach { item ->
                commanderRegistry.register(
                    CommanderAction(
                        label = item.displayName,
                        category = item.category,
                        action = { navigationItem = item },
                    ),
                )
            }

            commanderRegistry.registerAll(
                listOf(
                    CommanderAction("Connect Device…", "Device", "⌘⇧C"),
                    CommanderAction("Disconnect Device", "Device"),
                    CommanderAction("Screen Capture", "Device", "⌘⇧S"),
                    CommanderAction("Screen Record", "Device", "⌘⇧R"),
                    CommanderAction("Install APK…", "Device", "⌘⇧I"),
                    CommanderAction("Pull File…", "Device"),
                    CommanderAction("Push File…", "Device"),
                    CommanderAction("Restart Device", "Device"),
                    CommanderAction("About ADBStudio", "Help"),
                    CommanderAction("Check for Updates…", "Help"),
                    CommanderAction("Documentation", "Help"),
                    CommanderAction("Report Issue…", "Help"),
                    CommanderAction("System Theme", "Theme", action = { themeMode = ThemeMode.System }),
                    CommanderAction("Dark Theme", "Theme", action = { themeMode = ThemeMode.Dark }),
                    CommanderAction("Light Theme", "Theme", action = { themeMode = ThemeMode.Light }),
                ),
            )
        }

        MenuBar {
            NavigationMenu(
                current = navigationItem,
                onSelect = { navigationItem = it },
            )
            ThemeMenu(
                current = themeMode,
                onSelect = { themeMode = it },
            )
            DeviceMenu(deviceManager)
            HelpMenu()
        }

        CommanderHost(
            isOpen = commanderOpen,
            onDismiss = { commanderOpen = false },
            onActionSelected = { action ->
                action.action()
                commanderOpen = false
            },
            registry = commanderRegistry,
        ) {
            App(
                themeMode = themeMode,
                navigationItem = navigationItem,
                adbManager = adbManager,
                selectedDevice = deviceManager.selectedDevice,
            )
        }
    }
}

@Composable
private fun MenuBarScope.NavigationMenu(
    current: NavigationItem,
    onSelect: (NavigationItem) -> Unit,
) {
    Menu("Navigation") {
        Item("Apps") { onSelect(NavigationItem.Apps) }
        Item("Debug Info") { onSelect(NavigationItem.DebugInfo) }
        Item("Settings") { onSelect(NavigationItem.Settings) }
        Item("UI Inspector") { onSelect(NavigationItem.UiInspector) }
    }
}

@Composable
private fun MenuBarScope.ThemeMenu(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Menu("Theme") {
        Item("System") { onSelect(ThemeMode.System) }
        Item("Dark") { onSelect(ThemeMode.Dark) }
        Item("Light") { onSelect(ThemeMode.Light) }
    }
}

@Composable
private fun MenuBarScope.DeviceMenu(deviceManager: DeviceManager) {
    val devices = deviceManager.devices
    Menu("Device") {
        if (devices.isEmpty()) {
            Item("Refresh") { }
        } else {
            devices.forEach { device ->
                val isSelected = device.id == deviceManager.selectedDeviceId
                val prefix = if (isSelected) "✓ " else "  "
                Item("$prefix${device.id}") {
                    deviceManager.selectDevice(device.id)
                }
            }
            Separator()
            Item("Refresh") { }
        }
    }
}

@Composable
private fun MenuBarScope.HelpMenu() {
    Menu("Help") {
        Item("About ADBStudio") { }
        Item("Check for Updates…") { }
        Separator()
        Item("Documentation") { }
        Item("Report Issue…") { }
    }
}
