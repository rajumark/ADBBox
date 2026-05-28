package com.adbstudio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.adbstudio.desktop.core.events.SimpleEventBus
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.ThemeMode
import kotlin.time.TimeSource

fun main() = application {
    val adbManager = remember { AdbManager() }
    var commanderOpen by remember { mutableStateOf(false) }
    var lastShiftTime by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val doubleShiftThresholdMs = 400.0

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
        val eventBus = remember { SimpleEventBus() }
        val deviceRepository = remember { DeviceRepository(adbManager = adbManager, eventBus = eventBus) }

        LaunchedEffect(Unit) {
            deviceRepository.start(this)
        }

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
            DeviceMenu(deviceRepository = deviceRepository)
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
                deviceRepository = deviceRepository,
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
        Item("Devices") { onSelect(NavigationItem.Devices) }
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
private fun MenuBarScope.DeviceMenu(
    deviceRepository: DeviceRepository,
) {
    val scope = rememberCoroutineScope()
    val state by deviceRepository.state.collectAsState()

    Menu("Device") {
        if (!state.isAdbReady) {
            Item("ADB not ready") { }
            return@Menu
        }

        if (state.devices.isEmpty()) {
            Item("No devices") { }
        } else {
            state.devices.forEach { device ->
                val selectedPrefix = if (device.serial == state.selectedSerial) "✓ " else ""
                Item("$selectedPrefix${device.serial} (${device.state})") {
                    deviceRepository.selectDevice(device.serial)
                }
            }
        }

        Separator()
        Item("Refresh now") {
            deviceRepository.requestRefresh(scope)
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
