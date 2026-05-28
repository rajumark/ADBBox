package com.adbstudio.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbstudio.desktop.navigation.Screen
import com.adbstudio.desktop.theme.AdbStudioTheme
import com.adbstudio.desktop.theme.ThemeMode
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.apps.presentation.AppsViewModel
import com.adbstudio.desktop.feature.battery.presentation.BatteryViewModel
import com.adbstudio.desktop.feature.settings.presentation.SettingsViewModel
import com.adbstudio.desktop.feature.calendar.presentation.CalendarViewModel
import com.adbstudio.desktop.feature.contacts.presentation.ContactsViewModel
import com.adbstudio.desktop.feature.devicesettings.presentation.DeviceSettingsViewModel
import com.adbstudio.desktop.feature.deviceproperties.presentation.DevicePropertiesViewModel
import com.adbstudio.desktop.feature.inspector.presentation.UiInspectorViewModel
import com.adbstudio.desktop.feature.lifecycle.presentation.LifecycleViewModel
import com.adbstudio.desktop.feature.media.presentation.MediaViewModel
import com.adbstudio.desktop.feature.messages.presentation.MessagesViewModel
import com.adbstudio.desktop.feature.notification.presentation.NotificationViewModel
import com.adbstudio.desktop.feature.systemdetails.presentation.SystemDetailsViewModel
import com.adbstudio.desktop.ui.screen.AppsScreen
import com.adbstudio.desktop.ui.screen.BatteryScreen
import com.adbstudio.desktop.ui.screen.CalendarScreen
import com.adbstudio.desktop.ui.screen.ContactsScreen
import com.adbstudio.desktop.ui.screen.DevicePropertiesScreen
import com.adbstudio.desktop.ui.screen.DeviceSettingsScreen
import com.adbstudio.desktop.ui.screen.DebugInfoScreen
import com.adbstudio.desktop.ui.screen.DevicesPage
import com.adbstudio.desktop.ui.screen.LifecycleScreen
import com.adbstudio.desktop.ui.screen.MediaScreen
import com.adbstudio.desktop.ui.screen.MessagesScreen
import com.adbstudio.desktop.ui.screen.NotificationScreen
import com.adbstudio.desktop.ui.screen.SettingsScreen
import com.adbstudio.desktop.ui.screen.SystemDetailsScreen
import com.adbstudio.desktop.ui.screen.UiInspectorScreen
import org.koin.compose.koinInject

@Composable
fun App(
    themeMode: ThemeMode,
    navigationItem: Screen,
    adbManager: AdbManager = koinInject(),
    deviceRepository: DeviceRepository = koinInject(),
    appsViewModel: AppsViewModel = koinInject(),
    batteryViewModel: BatteryViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
    calendarViewModel: CalendarViewModel = koinInject(),
    contactsViewModel: ContactsViewModel = koinInject(),
    deviceSettingsViewModel: DeviceSettingsViewModel = koinInject(),
    devicePropertiesViewModel: DevicePropertiesViewModel = koinInject(),
    lifecycleViewModel: LifecycleViewModel = koinInject(),
    mediaViewModel: MediaViewModel = koinInject(),
    messagesViewModel: MessagesViewModel = koinInject(),
    notificationViewModel: NotificationViewModel = koinInject(),
    systemDetailsViewModel: SystemDetailsViewModel = koinInject(),
    uiInspectorViewModel: UiInspectorViewModel = koinInject(),
) {
    AdbStudioTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (navigationItem) {
                Screen.Devices -> DevicesPage(deviceRepository = deviceRepository)
                Screen.Apps -> AppsScreen(viewModel = appsViewModel)
                Screen.Battery -> BatteryScreen(viewModel = batteryViewModel)
                Screen.DebugInfo -> DebugInfoScreen(adbManager = adbManager)
                Screen.Settings -> SettingsScreen(viewModel = settingsViewModel)
                Screen.UiInspector -> UiInspectorScreen(viewModel = uiInspectorViewModel)
                Screen.Calendar -> CalendarScreen(viewModel = calendarViewModel)
                Screen.Contacts -> ContactsScreen(viewModel = contactsViewModel)
                Screen.Media -> MediaScreen(viewModel = mediaViewModel)
                Screen.Messages -> MessagesScreen(viewModel = messagesViewModel)
                Screen.Notifications -> NotificationScreen(viewModel = notificationViewModel)
                Screen.Lifecycle -> LifecycleScreen(viewModel = lifecycleViewModel)
                Screen.DeviceSettings -> DeviceSettingsScreen(viewModel = deviceSettingsViewModel)
                Screen.DeviceProperties -> DevicePropertiesScreen(viewModel = devicePropertiesViewModel)
                Screen.SystemDetails -> SystemDetailsScreen(viewModel = systemDetailsViewModel)
            }
        }
    }
}
