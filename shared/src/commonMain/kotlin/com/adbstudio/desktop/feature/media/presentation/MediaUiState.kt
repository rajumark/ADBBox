package com.adbstudio.desktop.feature.media.presentation

import com.adbstudio.desktop.feature.media.domain.MediaSourceType
import com.adbstudio.desktop.feature.media.domain.MediaContentType

data class MediaUiState(
    val selectedSerial: String? = null,
    val data: List<Map<String, String>> = emptyList(),
    val sourceType: MediaSourceType = MediaSourceType.External,
    val contentType: MediaContentType = MediaContentType.Images,
    val searchQuery: String = "",
    val showOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed class MediaEvent {
    object Refresh : MediaEvent()
    data class SetSourceType(val sourceType: MediaSourceType) : MediaEvent()
    data class SetContentType(val contentType: MediaContentType) : MediaEvent()
    data class SetSearchQuery(val query: String) : MediaEvent()
    object ToggleOriginal : MediaEvent()
}
