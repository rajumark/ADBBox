package com.adbstudio.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.AdbStudioTheme
import com.adbstudio.desktop.theme.ThemeMode
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.apps.presentation.AppsViewModel
import com.adbstudio.desktop.feature.battery.presentation.BatteryViewModel
import com.adbstudio.desktop.feature.settings.presentation.SettingsViewModel
import com.adbstudio.desktop.feature.calendar.presentation.CalendarViewModel
import com.adbstudio.desktop.feature.contacts.presentation.ContactsViewModel
import com.adbstudio.desktop.ui.screen.AppsScreen
import com.adbstudio.desktop.ui.screen.BatteryScreen
import com.adbstudio.desktop.ui.screen.CalendarScreen
import com.adbstudio.desktop.ui.screen.ContactsScreen
import com.adbstudio.desktop.ui.screen.DebugInfoScreen
import com.adbstudio.desktop.ui.screen.DevicesPage
import com.adbstudio.desktop.ui.screen.SettingsScreen
import com.adbstudio.desktop.ui.screen.UiInspectorScreen
import org.koin.compose.koinInject

@Composable
fun App(
    themeMode: ThemeMode,
    navigationItem: NavigationItem,
    adbManager: AdbManager = koinInject(),
    deviceRepository: DeviceRepository = koinInject(),
    appsViewModel: AppsViewModel = koinInject(),
    batteryViewModel: BatteryViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
    calendarViewModel: CalendarViewModel = koinInject(),
    contactsViewModel: ContactsViewModel = koinInject(),
    lifecycleViewModel: LifecycleViewModel = koinInject(),
    mediaViewModel: MediaViewModel = koinInject(),
) {
    AdbStudioTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (navigationItem) {
                NavigationItem.Devices -> DevicesPage(deviceRepository = deviceRepository)
                NavigationItem.Apps -> AppsScreen(viewModel = appsViewModel)
                NavigationItem.Battery -> BatteryScreen(viewModel = batteryViewModel)
                NavigationItem.DebugInfo -> DebugInfoScreen(adbManager = adbManager)
                NavigationItem.Settings -> SettingsScreen(viewModel = settingsViewModel)
                NavigationItem.UiInspector -> UiInspectorScreen()
                NavigationItem.Calendar -> CalendarScreen(viewModel = calendarViewModel)
                NavigationItem.Contacts -> ContactsScreen(viewModel = contactsViewModel)
            }
        }
    }
}
