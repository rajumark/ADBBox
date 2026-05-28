package com.adbstudio.desktop.navigation

/**
 * Marker interface for all app screens (§8).
 *
 * For 400+ screens, use per-feature sealed classes implementing this interface.
 * For current scale (~12 screens), a single sealed class is acceptable.
 */
interface AppScreen

/**
 * All navigation destinations in the app (§8).
 *
 * State-driven navigation — no 3rd-party nav libs.
 * Back stack stored as List<AppScreen> in NavigationViewModel.
 */
sealed class Screen(
    val displayName: String,
    val category: String = "Navigation",
) : AppScreen {
    data object Devices : Screen("Devices")
    data object Apps : Screen("Apps")
    data object Battery : Screen("Battery")
    data object DebugInfo : Screen("Debug Info")
    data object Settings : Screen("Settings")
    data object UiInspector : Screen("UI Inspector")
    data object Calendar : Screen("Calendar")
    data object Contacts : Screen("Contacts")
    data object Media : Screen("Media")
    data object Messages : Screen("Messages")
    data object Notifications : Screen("Notifications")
    data object Lifecycle : Screen("Lifecycle")
    data object DeviceSettings : Screen("Device Settings")
    data object DeviceProperties : Screen("Device Properties")
    data object SystemDetails : Screen("System Details")

    companion object {
        /** All available screens for command palette registration. */
        val entries: List<Screen> = listOf(
            Devices, Apps, Battery, DebugInfo, Settings, UiInspector,
            Calendar, Contacts, Media, Messages, Notifications, Lifecycle,
            DeviceSettings, DeviceProperties, SystemDetails,
        )
    }
}
