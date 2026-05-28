package com.adbstudio.desktop.feature.media.domain

data class MediaItem(
    val id: String,
    val displayName: String,
    val size: String,
    val mimeType: String,
    val dateAdded: String,
    val dateModified: String,
    val duration: String = "",
    val resolution: String = "",
    val width: String = "",
    val height: String = "",
    val dataPath: String = "",
    val allFields: Map<String, String?> = emptyMap()
)
