package com.adbstudio.desktop.feature.lifecycle.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.lifecycle.DumpsysLifecycleCommand
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.device.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LifecycleViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(LifecycleUiState())
    val state: StateFlow<LifecycleUiState> = _state.asStateFlow()

    init {
        scope.launch {
            deviceRepository.state
                .map { it.selectedSerial }
                .distinctUntilChanged()
                .collect { serial ->
                    _state.value = _state.value.copy(selectedSerial = serial)
                    refreshInternal(serial, isManual = false)
                }
        }
    }

    fun onEvent(event: LifecycleEvent) {
        when (event) {
            LifecycleEvent.Refresh -> refreshInternal(_state.value.selectedSerial, isManual = true)
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun refreshInternal(serial: String?, isManual: Boolean) {
        scope.launch {
            if (serial.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    logs = emptyList(),
                    isLoading = false,
                    errorMessage = "No device selected",
                )
                return@launch
            }

            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            when (val result = adbManager.run(DumpsysLifecycleCommand(serial))) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        logs = result.value,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        logs = emptyList(),
                        isLoading = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        }
    }
}
