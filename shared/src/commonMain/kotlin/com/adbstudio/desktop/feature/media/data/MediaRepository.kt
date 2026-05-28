package com.adbstudio.desktop.feature.media.data

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.media.MediaContentTypeUri
import com.adbstudio.desktop.adb.model.media.MediaSourceTypeUri
import com.adbstudio.desktop.adb.model.media.QueryMediaContentCommand
import com.adbstudio.desktop.core.result.AppResult

class MediaRepository(private val adbManager: AdbManager) {
    
    suspend fun getMedia(
        serial: String,
        sourceType: MediaSourceTypeUri,
        contentType: MediaContentTypeUri,
        showOriginal: Boolean = false,
    ): AppResult<List<Map<String, String>>> {
        val command = QueryMediaContentCommand(
            serial = serial,
            sourceType = sourceType,
            contentType = contentType,
            showOriginal = showOriginal,
        )
        return adbManager.run(command)
    }
}
