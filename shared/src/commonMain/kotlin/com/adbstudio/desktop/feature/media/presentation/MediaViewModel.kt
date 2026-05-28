package com.adbstudio.desktop.feature.media.presentation

import com.adbstudio.desktop.adb.model.media.MediaContentTypeUri
import com.adbstudio.desktop.adb.model.media.MediaSourceTypeUri
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.media.data.MediaRepository
import com.adbstudio.desktop.feature.media.domain.MediaContentType
import com.adbstudio.desktop.feature.media.domain.MediaSourceType
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

class MediaViewModel(
    private val mediaRepository: MediaRepository,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

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

    fun onEvent(event: MediaEvent) {
        when (event) {
            MediaEvent.Refresh -> refreshInternal(_state.value.selectedSerial, isManual = true)
            is MediaEvent.SetSourceType -> {
                _state.value = _state.value.copy(sourceType = event.sourceType)
                refreshInternal(_state.value.selectedSerial, isManual = false)
            }
            is MediaEvent.SetContentType -> {
                _state.value = _state.value.copy(contentType = event.contentType)
                refreshInternal(_state.value.selectedSerial, isManual = false)
            }
            is MediaEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            MediaEvent.ToggleOriginal -> {
                _state.value = _state.value.copy(showOriginal = !_state.value.showOriginal)
                refreshInternal(_state.value.selectedSerial, isManual = false)
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun refreshInternal(serial: String?, isManual: Boolean) {
        scope.launch {
            if (serial.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    data = emptyList(),
                    isLoading = false,
                    errorMessage = "No device selected",
                )
                return@launch
            }

            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            val sourceTypeUri = when (_state.value.sourceType) {
                MediaSourceType.External -> MediaSourceTypeUri.External
                MediaSourceType.Internal -> MediaSourceTypeUri.Internal
            }

            val contentTypeUri = when (_state.value.contentType) {
                MediaContentType.Images -> MediaContentTypeUri.Images
                MediaContentType.Audio -> MediaContentTypeUri.Audio
                MediaContentType.Video -> MediaContentTypeUri.Video
            }

            when (val result = mediaRepository.getMedia(
                serial = serial,
                sourceType = sourceTypeUri,
                contentType = contentTypeUri,
                showOriginal = _state.value.showOriginal,
            )) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        data = result.value,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        data = emptyList(),
                        isLoading = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        }
    }
}
