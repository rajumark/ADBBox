package com.adbstudio.desktop.adb.model.messages

import com.adbstudio.desktop.adb.model.base.AdbCommand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class QueryMessagesContentCommand(
    override val serial: String?,
    val showOriginal: Boolean = false,
) : AdbCommand<List<Map<String, String>>> {
    override val id: String = "shell.content.query.messages"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "content", "query", "--uri", "content://sms"))
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

            if (key in validMessagesColumns) {
                if (!showOriginal) {
                    value = formatMessagesValue(key, value)
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

    private fun formatMessagesValue(key: String, value: String): String {
        return when (key) {
            "type" -> getMessagesTypeDescription(value.toIntOrNull() ?: 99)
            "date", "date_sent" -> convertTimestampToDate(value)
            "read" -> getMessagesReadStatusDescription(value.toIntOrNull() ?: 99)
            else -> value
        }
    }

    private fun convertTimestampToDate(timestamp: String): String {
        return try {
            val date = Date(timestamp.toLong())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getDefault()
            dateFormat.format(date)
        } catch (_: Exception) {
            timestamp
        }
    }

    companion object {
        val messagesColumnSortOrder = listOf(
            "type", "date", "address", "body", "read", "seen",
        )
    }
}

private fun getMessagesTypeDescription(messageType: Int): String {
    return when (messageType) {
        1 -> "Incoming SMS"
        2 -> "Sent SMS"
        3 -> "Draft SMS"
        4 -> "Failed SMS"
        else -> "Unknown Message Type(type=$messageType)"
    }
}

private fun getMessagesReadStatusDescription(readStatus: Int): String {
    return when (readStatus) {
        1 -> "Read"
        0 -> "Unread"
        else -> "Unknown Read Status(readStatus=$readStatus)"
    }
}

private val validMessagesColumns = setOf(
    "_id", "thread_id", "address", "person", "date", "date_sent",
    "protocol", "read", "status", "type", "reply_path_present",
    "subject", "body", "service_center", "locked", "error_code",
    "seen", "timed", "deleted", "sync_state", "marker", "source",
    "bind_id", "mx_status", "mx_id", "out_time", "account", "sim_id",
    "block_type", "advanced_seen", "b2c_ttl", "b2c_numbers",
    "fake_cell_type", "url_risky_type", "creator", "favorite_date",
    "mx_id_v2", "sub_id",
)
