package com.adbstudio.desktop.device

/**
 * Device capabilities detected at connection time (§9).
 *
 * Replaces scattered `if (apiLevel >= 34)` checks across features.
 * Created once per device connection, invalidated on disconnect.
 */
sealed interface DeviceCapability {
    data object WirelessDebugging : DeviceCapability
    data object IncrementalInstall : DeviceCapability
    data object ScreenRecording : DeviceCapability
    data object RootAccess : DeviceCapability
    data object TcpipMode : DeviceCapability
    data object Bugreport : DeviceCapability
    data object ScreenMirroring : DeviceCapability
}

/**
 * Connection type for the device.
 */
enum class ConnectionType(val displayName: String) {
    USB("USB"),
    WIFI("Wi-Fi"),
    EMULATOR("Emulator"),
}

/**
 * Cached device state created on connection, invalidated on disconnect (§9).
 *
 * - ADB queries are expensive — this caches results with TTL.
 * - Capabilities replace `if (apiLevel >= X)` scattered across features.
 * - Features request sessions by serial via [DeviceRepository].
 */
data class DeviceSession(
    val serial: String,
    val apiLevel: Int,
    val features: Set<DeviceCapability>,
    val connectionType: ConnectionType,
    val cachedProperties: Map<String, String>,
    val model: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val sdkVersion: String = "",
) {
    /** Check if this device supports a given capability. */
    fun supports(capability: DeviceCapability): Boolean = capability in features

    /** Check if this device meets a minimum API level requirement. */
    fun meetsApiLevel(minApi: Int): Boolean = apiLevel >= minApi
}
