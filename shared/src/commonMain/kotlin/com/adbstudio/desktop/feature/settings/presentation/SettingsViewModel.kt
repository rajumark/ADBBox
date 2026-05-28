package com.adbstudio.desktop.feature.settings.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.settings.OpenAndroidSettingsCommand
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.settings.data.SettingsItem
import com.adbstudio.desktop.feature.settings.data.SettingsRegistry
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

class SettingsViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        val categories = SettingsRegistry.categories
        _state.value = _state.value.copy(
            categories = categories,
            selectedCategory = categories.firstOrNull(),
        )

        scope.launch {
            deviceRepository.state
                .map { it.selectedSerial }
                .distinctUntilChanged()
                .collect { serial ->
                    _state.value = _state.value.copy(selectedSerial = serial)
                }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SelectCategory -> {
                _state.value = _state.value.copy(
                    selectedCategory = event.category,
                    searchQuery = "",
                )
            }
            is SettingsEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            is SettingsEvent.OpenSetting -> {
                openSetting(event.item)
            }
            SettingsEvent.DismissMessage -> {
                _state.value = _state.value.copy(actionMessage = null, errorMessage = null)
            }
        }
    }

    private fun openSetting(item: SettingsItem) {
        val serial = _state.value.selectedSerial
        if (serial.isNullOrBlank()) {
            _state.value = _state.value.copy(errorMessage = "No device selected")
            return
        }

        scope.launch {
            _state.value = _state.value.copy(actionMessage = "Opening ${item.displayName}...")
            when (val result = adbManager.run(OpenAndroidSettingsCommand(serial, item.intent))) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(actionMessage = "Opened ${item.displayName}")
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        errorMessage = "Failed to open ${item.displayName}: ${result.error.toUserMessage()}",
                        actionMessage = null,
                    )
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
