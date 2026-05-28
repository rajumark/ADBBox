package com.adbstudio.desktop.feature.media.domain

enum class MediaSourceType(val title: String, val value: String) {
    External("External", "external"),
    Internal("Internal", "internal");

    override fun toString(): String = title
}

enum class MediaContentType(val title: String, val value: String) {
    Images("Images", "images"),
    Audio("Audio", "audio"),
    Video("Video", "video");

    override fun toString(): String = title
}
