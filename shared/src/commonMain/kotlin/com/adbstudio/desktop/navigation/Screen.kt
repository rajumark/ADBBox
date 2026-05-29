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
sealed class ScreenPage(
    val displayName: String,
    val category: String = "Navigation",
) : AppScreen {
    object Devices : ScreenPage("Devices")
    object Apps : ScreenPage("Apps")
    object Battery : ScreenPage("Battery")
    object DebugInfo : ScreenPage("Debug Info")
    object Settings : ScreenPage("Settings")
    object UiInspector : ScreenPage("UI Inspector")
    object Calendar : ScreenPage("Calendar")
    object Contacts : ScreenPage("Contacts")
    object Media : ScreenPage("Media")
    object Messages : ScreenPage("Messages")
    object Notifications : ScreenPage("Notifications")
    object Lifecycle : ScreenPage("Lifecycle")
    object DeviceSettings : ScreenPage("Device Settings")
    object DeviceProperties : ScreenPage("Device Properties")
    object SystemDetails : ScreenPage("System Details")
    object Processes : ScreenPage("Processes")

    companion object
}
