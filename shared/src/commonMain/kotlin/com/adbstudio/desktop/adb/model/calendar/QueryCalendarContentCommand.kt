package com.adbstudio.desktop.adb.model.calendar

import com.adbstudio.desktop.adb.model.base.AdbCommand

enum class CalendarQueryType(val uri: String) {
    EVENTS("content://com.android.calendar/events"),
    CALENDARS("content://com.android.calendar/calendars"),
}

data class QueryCalendarContentCommand(
    override val serial: String?,
    val queryType: CalendarQueryType,
    val showOriginal: Boolean = false,
) : AdbCommand<List<Map<String, String>>> {
    override val id: String = "shell.content.query.${queryType.name.lowercase()}"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "content", "query", "--uri", queryType.uri))
    }

    override fun parse(output: String): List<Map<String, String>> {
        return output
            .lineSequence()
            .mapNotNull { line ->
                val row = line.substringAfter("Row: ").trim()
                if (row.isBlank() || !row.contains("=")) return@mapNotNull null
                parseRow(row, queryType, showOriginal)
            }
            .toList()
    }

    private fun parseRow(row: String, type: CalendarQueryType, showOriginal: Boolean): Map<String, String>? {
        val dataMap = mutableMapOf<String, String>()
        val fields = row.split(", ").map { it.trim() }

        for ((index, field) in fields.withIndex()) {
            val keyValue = field.split("=", limit = 2)
            if (keyValue.size != 2) continue

            val key = keyValue[0].trim()
            var value = keyValue[1].trim()

            if (value == "NULL") continue

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

            val validKeys = if (type == CalendarQueryType.EVENTS) validEventColumns else validCalendarColumns
            if (key in validKeys) {
                if (!showOriginal) {
                    value = formatCalendarValue(key, value)
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

    private fun formatCalendarValue(key: String, value: String): String {
        if (key == "dtstart" || key == "dtend" || key == "lastDate") {
            return try {
                val millis = value.toLong()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                sdf.format(java.util.Date(millis))
            } catch (_: NumberFormatException) {
                value
            }
        }
        if (key == "date_added" || key == "date_modified") {
            return try {
                val seconds = value.toLong() * 1000
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                sdf.format(java.util.Date(seconds))
            } catch (_: NumberFormatException) {
                value
            }
        }
        return value
    }

    companion object {
        val validEventColumns = listOf(
            "originalAllDay", "account_type", "exrule", "mutators", "originalInstanceTime",
            "allDay", "allowedReminders", "rrule", "canOrganizerRespond", "lastDate",
            "visible", "calendar_id", "hasExtendedProperties", "calendar_access_level",
            "selfAttendeeStatus", "allowedAvailability", "eventColor_index", "isOrganizer",
            "_sync_id", "calendar_color_index", "_id", "guestsCanInviteOthers",
            "allowedAttendeeTypes", "dtstart", "guestsCanSeeGuests", "sync_data9",
            "sync_data8", "exdate", "sync_data7", "sync_data6", "sync_data1",
            "description", "eventTimezone", "availability", "title", "ownerAccount",
            "sync_data5", "sync_data4", "sync_data3", "sync_data2", "duration",
            "lastSynced", "guestsCanModify", "cal_sync3", "rdate", "cal_sync2",
            "maxReminders", "isPrimary", "cal_sync1", "cal_sync10", "account_name",
            "cal_sync7", "cal_sync6", "cal_sync5", "cal_sync4", "calendar_color",
            "cal_sync9", "cal_sync8", "dirty", "calendar_timezone", "accessLevel",
            "eventLocation", "hasAlarm", "uid2445", "deleted", "eventColor",
            "organizer", "eventStatus", "customAppUri", "canModifyTimeZone",
            "eventEndTimezone", "customAppPackage", "original_sync_id",
            "hasAttendeeData", "displayColor", "dtend", "original_id",
            "sync_data10", "calendar_displayName",
        )

        val validCalendarColumns = listOf(
            "account_type", "mutators", "ownerAccount", "allowedReminders",
            "cal_sync3", "cal_sync2", "isPrimary", "maxReminders", "cal_sync1",
            "cal_sync10", "account_name", "cal_sync7", "cal_sync6",
            "canPartiallyUpdate", "cal_sync5", "sync_events", "cal_sync4",
            "canOrganizerRespond", "calendar_color", "cal_sync9",
            "calendar_location", "cal_sync8", "dirty", "visible",
            "calendar_timezone", "calendar_access_level", "allowedAvailability",
            "_sync_id", "deleted", "name", "canModifyTimeZone", "_id",
            "calendar_color_index", "allowedAttendeeTypes", "calendar_displayName",
            "date_added", "date_modified",
        )

        val eventColumnSortOrder = listOf(
            "_id", "dtstart", "title", "dtend", "sync_data5", "account_name",
            "lastDate", "eventTimezone", "description", "organizer", "calendar_displayName",
        )
    }
}
