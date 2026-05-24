package com.adbstudio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.commander.CommanderAction
import com.adbstudio.desktop.commander.CommanderHost
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.ThemeMode

fun main() = application {
    val adbManager = remember { AdbManager() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ADBStudio",
    ) {
        var themeMode by remember { mutableStateOf(ThemeMode.System) }
        var navigationItem by remember { mutableStateOf(NavigationItem.Apps) }
        val commanderRegistry = remember { CommanderRegistry() }

        MenuBar {
            NavigationMenu(
                current = navigationItem,
                onSelect = { navigationItem = it },
            )
            ThemeMenu(
                current = themeMode,
                onSelect = { themeMode = it },
            )
            DeviceMenu()
            HelpMenu()
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

        CommanderHost(registry = commanderRegistry) {
            App(
                themeMode = themeMode,
                navigationItem = navigationItem,
                adbManager = adbManager,
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
private fun MenuBarScope.DeviceMenu() {
    Menu("Device") {
        Item("Connect Device…") { }
        Item("Disconnect Device") { }
        Separator()
        Item("Screen Capture") { }
        Item("Screen Record") { }
        Separator()
        Item("Install APK…") { }
        Item("Pull File…") { }
        Item("Push File…") { }
        Separator()
        Item("Restart Device") { }
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
