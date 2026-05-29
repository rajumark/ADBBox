package com.adbstudio.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.adbstudio.desktop.feature.devicesettings.presentation.DeviceSettingsViewModel
import com.adbstudio.desktop.feature.deviceproperties.presentation.DevicePropertiesViewModel
import com.adbstudio.desktop.feature.inspector.presentation.UiInspectorViewModel
import com.adbstudio.desktop.feature.lifecycle.presentation.LifecycleViewModel
import com.adbstudio.desktop.feature.media.presentation.MediaViewModel
import com.adbstudio.desktop.feature.messages.presentation.MessagesViewModel
import com.adbstudio.desktop.feature.notification.presentation.NotificationViewModel
import com.adbstudio.desktop.feature.processes.presentation.ProcessesViewModel
import com.adbstudio.desktop.feature.systemdetails.presentation.SystemDetailsViewModel
import com.adbstudio.desktop.navigation.ScreenPage
import com.adbstudio.desktop.theme.ThemeMode
import com.adbstudio.desktop.ui.component.ErrorDialog
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import kotlin.time.TimeSource

private val globalError = MutableStateFlow<Throwable?>(null)

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        globalError.value = throwable
    }

    application {
        val crashError by globalError.collectAsState()

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
            var navigationItem by remember { mutableStateOf<ScreenPage>(ScreenPage.Apps) }
            val appScope = rememberCoroutineScope()
            val commanderRegistry = koinInject<CommanderRegistry>()
            val deviceRepository = koinInject<DeviceRepository>()
            val appsViewModel = koinInject<AppsViewModel>()
            val batteryViewModel = koinInject<BatteryViewModel>()
            val calendarViewModel = koinInject<CalendarViewModel>()
            val contactsViewModel = koinInject<ContactsViewModel>()
            val deviceSettingsViewModel = koinInject<DeviceSettingsViewModel>()
            val devicePropertiesViewModel = koinInject<DevicePropertiesViewModel>()
            val lifecycleViewModel = koinInject<LifecycleViewModel>()
            val mediaViewModel = koinInject<MediaViewModel>()
            val messagesViewModel = koinInject<MessagesViewModel>()
            val notificationViewModel = koinInject<NotificationViewModel>()
            val systemDetailsViewModel = koinInject<SystemDetailsViewModel>()
            val uiInspectorViewModel = koinInject<UiInspectorViewModel>()
            val processesViewModel = koinInject<ProcessesViewModel>()

            DisposableEffect(Unit) {
                deviceRepository.start(appScope)
                onDispose {
                    deviceRepository.stop()
                    appsViewModel.close()
                    batteryViewModel.close()
                    calendarViewModel.close()
                    contactsViewModel.close()
                    deviceSettingsViewModel.close()
                    devicePropertiesViewModel.close()
                    lifecycleViewModel.close()
                    mediaViewModel.close()
                    messagesViewModel.close()
                    notificationViewModel.close()
                    systemDetailsViewModel.close()
                    uiInspectorViewModel.close()
                    processesViewModel.close()
                }
            }

            LaunchedEffect(Unit) {
                listOf(
                    ScreenPage.Devices to "Devices",
                    ScreenPage.Apps to "Apps",
                    ScreenPage.Battery to "Battery",
                    ScreenPage.DebugInfo to "Debug Info",
                    ScreenPage.Settings to "Settings",
                    ScreenPage.UiInspector to "UI Inspector",
                    ScreenPage.Calendar to "Calendar",
                    ScreenPage.Contacts to "Contacts",
                    ScreenPage.Media to "Media",
                    ScreenPage.Messages to "Messages",
                    ScreenPage.Notifications to "Notifications",
                    ScreenPage.Lifecycle to "Lifecycle",
                    ScreenPage.DeviceSettings to "Device Settings",
                    ScreenPage.DeviceProperties to "Device Properties",
                    ScreenPage.SystemDetails to "System Details",
                    ScreenPage.Processes to "Processes",
                ).forEach { (screen, label) ->
                    commanderRegistry.register(
                        CommanderAction(
                            label = label,
                            category = "Navigation",
                            action = { navigationItem = screen },
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

            Box(Modifier.fillMaxSize()) {
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
                        deviceSettingsViewModel = deviceSettingsViewModel,
                        devicePropertiesViewModel = devicePropertiesViewModel,
                        lifecycleViewModel = lifecycleViewModel,
                        mediaViewModel = mediaViewModel,
                        messagesViewModel = messagesViewModel,
                        notificationViewModel = notificationViewModel,
                        systemDetailsViewModel = systemDetailsViewModel,
                        uiInspectorViewModel = uiInspectorViewModel,
                        processesViewModel = processesViewModel,
                    )
                }

                crashError?.let { error ->
                    ErrorDialog(
                        throwable = error,
                        onDismiss = { globalError.value = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuBarScope.NavigationMenu(
    current: ScreenPage,
    onSelect: (ScreenPage) -> Unit,
) {
    Menu("Navigation") {
        Item("Devices") { onSelect(ScreenPage.Devices) }
        Item("Apps") { onSelect(ScreenPage.Apps) }
        Item("Battery") { onSelect(ScreenPage.Battery) }
        Item("Calendar") { onSelect(ScreenPage.Calendar) }
        Item("Contacts") { onSelect(ScreenPage.Contacts) }
        Item("Media") { onSelect(ScreenPage.Media) }
        Item("Messages") { onSelect(ScreenPage.Messages) }
        Item("Notifications") { onSelect(ScreenPage.Notifications) }
        Item("Lifecycle") { onSelect(ScreenPage.Lifecycle) }
        Item("Device Settings") { onSelect(ScreenPage.DeviceSettings) }
        Item("Device Properties") { onSelect(ScreenPage.DeviceProperties) }
        Item("System Details") { onSelect(ScreenPage.SystemDetails) }
        Item("Debug Info") { onSelect(ScreenPage.DebugInfo) }
        Item("Settings") { onSelect(ScreenPage.Settings) }
        Item("UI Inspector") { onSelect(ScreenPage.UiInspector) }
        Item("Processes") { onSelect(ScreenPage.Processes) }
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
