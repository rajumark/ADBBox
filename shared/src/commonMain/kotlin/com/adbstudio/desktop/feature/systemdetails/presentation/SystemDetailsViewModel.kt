package com.adbstudio.desktop.feature.systemdetails.presentation

import com.adbstudio.desktop.adb.AdbManager
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

class SystemDetailsViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(SystemDetailsUiState())
    val state: StateFlow<SystemDetailsUiState> = _state.asStateFlow()

    init {
        scope.launch {
            deviceRepository.state
                .map { it.selectedSerial }
                .distinctUntilChanged()
                .collect { serial ->
                    _state.value = _state.value.copy(selectedSerial = serial)
                    if (!serial.isNullOrBlank()) {
                        refreshInternal(serial, isManual = false)
                    } else {
                        _state.value = SystemDetailsUiState(
                            selectedSerial = null,
                            errorMessage = "No device selected",
                        )
                    }
                }
        }
    }

    fun onEvent(event: SystemDetailsEvent) {
        when (event) {
            SystemDetailsEvent.Refresh -> {
                val serial = _state.value.selectedSerial
                if (!serial.isNullOrBlank()) refreshInternal(serial, isManual = true)
            }
            is SystemDetailsEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            is SystemDetailsEvent.SetTab -> {
                _state.value = _state.value.copy(currentTab = event.tab)
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun refreshInternal(serial: String, isManual: Boolean) {
        scope.launch {
            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            val features = fetchList(serial, DetailsTab.Features)
            val permissions = fetchList(serial, DetailsTab.Permissions)
            val libraries = fetchList(serial, DetailsTab.Libraries)

            val firstError = features.error ?: permissions.error ?: libraries.error

            _state.value = _state.value.copy(
                features = features.data,
                permissions = permissions.data,
                libraries = libraries.data,
                isLoading = false,
                errorMessage = firstError,
            )
        }
    }

    private suspend fun fetchList(serial: String, tab: DetailsTab): ListResult {
        return when (val result = adbManager.runShell(serial, tab.command)) {
            is AppResult.Success -> {
                val prefix = tab.prefix
                val parsed = result.value.lineSequence()
                    .map { it.trim() }
                    .filter { it.startsWith(prefix) }
                    .map { it.removePrefix(prefix) }
                    .toList()
                ListResult(parsed, null)
            }
            is AppResult.Error -> ListResult(emptyList(), result.error.toUserMessage())
        }
    }

    private data class ListResult(
        val data: List<String>,
        val error: String?,
    )
}
