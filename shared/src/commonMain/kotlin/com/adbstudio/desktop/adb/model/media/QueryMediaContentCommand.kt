package com.adbstudio.desktop.adb.model.media

import com.adbstudio.desktop.adb.model.base.AdbCommand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class MediaContentTypeUri(val value: String) {
    Images("images"),
    Audio("audio"),
    Video("video"),
}

enum class MediaSourceTypeUri(val value: String) {
    External("external"),
    Internal("internal"),
}

data class QueryMediaContentCommand(
    override val serial: String?,
    val sourceType: MediaSourceTypeUri,
    val contentType: MediaContentTypeUri,
    val showOriginal: Boolean = false,
) : AdbCommand<List<Map<String, String>>> {
    override val id: String = 
        "shell.content.query.media.${sourceType.value}.${contentType.value}"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf(
            "shell", "content", "query",
            "--uri", "content://media/${sourceType.value}/${contentType.value}/media"
        ))
    }

    override fun parse(output: String): List<Map<String, String>> {
        return output
            .lineSequence()
            .mapNotNull { line ->
                val row = line.substringAfter("Row: ").trim()
                if (row.isBlank() || !row.contains("=")) return@mapNotNull null
                parseRow(row, showOriginal)
            }
            .sortedByDescending { it["_id"]?.toLongOrNull() ?: 0L }
            .toList()
    }

    private fun parseRow(row: String, showOriginal: Boolean): Map<String, String>? {
        val dataMap = mutableMapOf<String, String>()
        val fields = row.split(", ").map { it.trim() }

        for ((index, field) in fields.withIndex()) {
            val keyValue = field.split("=", limit = 2)
            if (keyValue.size != 2) continue

            val key = keyValue[0].trim()
            var value = keyValue[1].trim()

            if (value == "NULL" || value.isEmpty()) continue

            var extra = getExtraValue(fields, index + 1)
            if (extra.isNotBlank()) {
                value = "$value, $extra"
                extra = getExtraValue(fields, index + 2)
                if (extra.isNotBlank()) {
                    value = "$value, $extra"
                    extra = getExtraValue(fields, index + 3)
                    if (extra.isNotBlank()) {
                        value = "$value, $extra"
                    }
                }
            }

            if (key in validMediaColumns) {
                if (!showOriginal) {
                    value = formatMediaValue(key, value)
                }
                dataMap[key] = value
            }
        }

        return if (dataMap.isNotEmpty()) dataMap else null
    }

    private fun getExtraValue(fields: List<String>, index: Int): String {
        val next = fields.getOrNull(index) ?: return ""
        return if (!next.contains("=")) next else ""
    }

    private fun formatMediaValue(key: String, value: String): String {
        return when (key) {
            "date_added", "date_modified" -> convertTimestampToDate(value, isSeconds = true)
            "datetaken" -> convertTimestampToDate(value, isSeconds = false)
            else -> value
        }
    }

    private fun convertTimestampToDate(timestamp: String, isSeconds: Boolean): String {
        return try {
            val millis = if (isSeconds) {
                timestamp.toLong() * 1000
            } else {
                timestamp.toLong()
            }
            val date = Date(millis)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getDefault()
            dateFormat.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }
}

private val validMediaColumns = setOf(
    "_id", "_display_name", "_size", "mime_type",
    "date_added", "date_modified", "datetaken",
    "duration", "resolution", "width", "height",
    "title", "album", "artist", "genre",
    "album_artist", "composer", "is_music", "is_podcast",
    "is_audiobook", "is_alarm", "is_ringtone", "is_notification",
    "is_recording", "is_favorite", "is_trashed",
    "latitude", "longitude", "orientation",
    "exposure_time", "f_number", "iso", "focal_length",
    "color_range", "color_standard", "color_transfer",
    "scene_capture_type", "bucket_id", "bucket_display_name",
    "relative_path", "_data", "owner_package_name",
    "capture_framerate", "bitrate", "track", "year",
    "author", "writer", "description"
)
