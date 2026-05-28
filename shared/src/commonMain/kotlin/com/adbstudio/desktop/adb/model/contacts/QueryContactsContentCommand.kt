package com.adbstudio.desktop.adb.model.contacts

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class QueryContactsContentCommand(
    override val serial: String?,
) : AdbCommand<List<Map<String, String>>> {
    override val id: String = "shell.content.query.contacts"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "content", "query", "--uri", "content://com.android.contacts/data"))
    }

    override fun parse(output: String): List<Map<String, String>> {
        return output
            .lineSequence()
            .mapNotNull { line ->
                val row = line.substringAfter("Row: ").trim()
                if (row.isBlank() || !row.contains("=")) return@mapNotNull null
                parseRow(row)
            }
            .toList()
    }

    private fun parseRow(row: String): Map<String, String>? {
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

            if (key in validContactColumns) {
                dataMap[key] = value
            }
        }

        return if (dataMap.isNotEmpty()) dataMap else null
    }

    private fun getExtraValue(fields: List<String>, index: Int): String {
        val next = fields.getOrNull(index) ?: return ""
        return if (!next.contains("=")) next else ""
    }

    companion object {
        val validContactColumns = listOf(
            "phonetic_name", "status_res_package", "custom_ringtone", "contact_status_ts",
            "account_type", "data_version", "photo_file_id", "contact_status_res_package",
            "name_verified", "group_sourceid", "display_name_alt", "sort_key_alt",
            "mode", "last_time_used", "starred", "contact_status_label",
            "has_phone_number", "chat_capability", "raw_contact_id", "contact_account_type",
            "carrier_presence", "contact_last_updated_timestamp", "res_package", "photo_uri",
            "data_sync4", "phonebook_bucket", "times_used", "display_name", "sort_key",
            "data_sync1", "version", "data_sync2", "data_sync3", "photo_thumb_uri",
            "status_label", "contact_presence", "in_default_directory", "times_contacted",
            "_id", "account_type_and_data_set", "name_raw_contact_id", "status",
            "phonebook_bucket_alt", "last_time_contacted", "pinned", "is_primary",
            "photo_id", "video", "contact_id", "contact_chat_capability",
            "contact_status_icon", "in_visible_group", "phonebook_label", "account_name",
            "nickname", "display_name_source", "company", "data9", "dirty", "sourceid",
            "phonetic_name_style", "send_to_voicemail", "data8", "lookup", "data7",
            "data6", "phonebook_label_alt", "data5", "is_super_primary", "data4",
            "data3", "data2", "data1", "data_set", "contact_status", "backup_id",
            "preferred_phone_account_component_name", "raw_contact_is_user_profile",
            "status_ts", "data10", "preferred_phone_account_id", "data12", "mimetype",
            "status_icon", "data11", "data14", "data13", "hash_id", "data15",
        )

        const val MIMETYPE_PHONE_V2 = "vnd.android.cursor.item/phone_v2"
        const val CONTACT_ID = "contact_id"
        const val DISPLAY_NAME = "display_name"
        const val MIMETYPE = "mimetype"
        const val DATA1 = "data1"
    }
}
