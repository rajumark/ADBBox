package com.adbstudio.desktop.device

import com.adbstudio.desktop.adb.AdbDevice
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.device.ListDevicesCommand
import com.adbstudio.desktop.core.error.AppError
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.events.AppEvent
import com.adbstudio.desktop.core.events.EventBus
import com.adbstudio.desktop.core.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DevicesState(
    val isAdbReady: Boolean = false,
    val devices: List<AdbDevice> = emptyList(),
    val selectedSerial: String? = null,
    /**
     * Only for user-triggered refresh. Background polling must not toggle UI states (prevents flicker).
     */
    val isManualRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Single source of truth for connected devices + selected device.
 *
 * - Independent of screens (start it once at app root).
 * - Polls every 3 seconds.
 * - Publishes device/selection changes via [EventBus] so other features can react.
 */
class DeviceRepository(
    private val adbManager: AdbManager,
    private val eventBus: EventBus,
) {
    private val _state = MutableStateFlow(DevicesState())
    val state: StateFlow<DevicesState> = _state.asStateFlow()

    fun events(): Flow<AppEvent> = eventBus.events()

    private var pollJob: Job? = null
    private var appScope: CoroutineScope? = null

    fun start(scope: CoroutineScope) {
        if (pollJob != null) return
        appScope = scope
        pollJob = scope.launch {
            // initial load immediately
            refreshOnce(isManual = false)
            while (isActive) {
                delay(3_000)
                refreshOnce(isManual = false)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        appScope = null
    }

    fun selectDevice(serial: String?) {
        val current = _state.value.selectedSerial
        if (serial == current) return

        // Only allow selecting existing device, or null.
        val allowed = serial == null || _state.value.devices.any { it.serial == serial }
        if (!allowed) return

        _state.update { it.copy(selectedSerial = serial) }
        // best-effort event publish
        publishAsync(AppEvent.SelectedDeviceChanged(serial))
    }

    fun requestRefresh(scope: CoroutineScope) {
        scope.launch { refreshOnce(isManual = true) }
    }

    private suspend fun refreshOnce(isManual: Boolean) {
        if (!adbManager.isReady) {
            setDevices(
                isAdbReady = false,
                devices = emptyList(),
                errorMessage = "ADB is not ready",
            )
            return
        }

        if (isManual) {
            _state.update { it.copy(isAdbReady = true, isManualRefreshing = true, errorMessage = null) }
        } else {
            // Polling should not clear errors or toggle loading states.
            _state.update { it.copy(isAdbReady = true) }
        }
        try {
            when (val result = adbManager.run(ListDevicesCommand())) {
                is AppResult.Success -> setDevices(isAdbReady = true, devices = result.value, errorMessage = null)
                is AppResult.Error -> {
                    val adbReady = result.error !is AppError.AdbNotReady
                    setDevices(
                        isAdbReady = adbReady,
                        devices = emptyList(),
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        } catch (t: Throwable) {
            setDevices(
                isAdbReady = true,
                devices = emptyList(),
                errorMessage = t.message ?: "Failed to fetch devices",
            )
        } finally {
            if (isManual) {
                _state.update { it.copy(isManualRefreshing = false) }
            }
        }
    }

    private fun setDevices(isAdbReady: Boolean, devices: List<AdbDevice>, errorMessage: String?) {
        val prev = _state.value

        val normalizedSelected = normalizeSelection(
            currentSelected = prev.selectedSerial,
            devices = devices,
        )

        val next = prev.copy(
            isAdbReady = isAdbReady,
            devices = devices,
            selectedSerial = normalizedSelected,
            errorMessage = errorMessage,
        )

        // Detect changes for events
        val listChanged = prev.devices != next.devices
        val selectionChanged = prev.selectedSerial != next.selectedSerial
        val errorChanged = prev.errorMessage != next.errorMessage
        val adbReadyChanged = prev.isAdbReady != next.isAdbReady

        // Avoid disturbing the UI if nothing important changed (prevents flicker on 3s polling).
        if (!listChanged && !selectionChanged && !errorChanged && !adbReadyChanged) {
            return
        }

        _state.value = next

        if (listChanged) publishAsync(AppEvent.DeviceListUpdated(next.devices))
        if (selectionChanged) publishAsync(AppEvent.SelectedDeviceChanged(next.selectedSerial))
    }

    private fun normalizeSelection(currentSelected: String?, devices: List<AdbDevice>): String? {
        if (devices.isEmpty()) return null
        if (currentSelected == null) return devices.first().serial
        if (devices.any { it.serial == currentSelected }) return currentSelected
        return devices.first().serial
    }

    private fun publishAsync(event: AppEvent) {
        // Avoid forcing callers to be in a suspend context.
        appScope?.launch {
            eventBus.publish(event)
        }
    }
}
