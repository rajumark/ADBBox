package com.adbstudio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.adbstudio.desktop.core.di.appModules
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.apps.presentation.AppsViewModel
import com.adbstudio.desktop.feature.battery.presentation.BatteryViewModel
import com.adbstudio.desktop.feature.calendar.presentation.CalendarViewModel
import com.adbstudio.desktop.feature.contacts.presentation.ContactsViewModel
import com.adbstudio.desktop.feature.lifecycle.presentation.LifecycleViewModel
import com.adbstudio.desktop.feature.media.presentation.MediaViewModel
import com.adbstudio.desktop.feature.messages.presentation.MessagesViewModel
import com.adbstudio.desktop.feature.notification.presentation.NotificationViewModel
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.ThemeMode
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import kotlin.time.TimeSource

fun main() = application {
    startKoin {
        modules(appModules)
    }

    val adbManager = koinInject<AdbManager>()
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
        val appScope = rememberCoroutineScope()
        val commanderRegistry = koinInject<CommanderRegistry>()
        val deviceRepository = koinInject<DeviceRepository>()
        val appsViewModel = koinInject<AppsViewModel>()
        val batteryViewModel = koinInject<BatteryViewModel>()
        val calendarViewModel = koinInject<CalendarViewModel>()
        val contactsViewModel = koinInject<ContactsViewModel>()
        val lifecycleViewModel = koinInject<LifecycleViewModel>()
        val mediaViewModel = koinInject<MediaViewModel>()
        val notificationViewModel = koinInject<NotificationViewModel>()

        DisposableEffect(Unit) {
            deviceRepository.start(appScope)
            onDispose {
                deviceRepository.stop()
                appsViewModel.close()
                batteryViewModel.close()
                calendarViewModel.close()
                contactsViewModel.close()
                lifecycleViewModel.close()
                mediaViewModel.close()
                notificationViewModel.close()
            }
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
                appsViewModel = appsViewModel,
                batteryViewModel = batteryViewModel,
                calendarViewModel = calendarViewModel,
                contactsViewModel = contactsViewModel,
                lifecycleViewModel = lifecycleViewModel,
                mediaViewModel = mediaViewModel,
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
        Item("Battery") { onSelect(NavigationItem.Battery) }
        Item("Calendar") { onSelect(NavigationItem.Calendar) }
        Item("Contacts") { onSelect(NavigationItem.Contacts) }
        Item("Media") { onSelect(NavigationItem.Media) }
        Item("Notifications") { onSelect(NavigationItem.Notifications) }
        Item("Lifecycle") { onSelect(NavigationItem.Lifecycle) }
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
