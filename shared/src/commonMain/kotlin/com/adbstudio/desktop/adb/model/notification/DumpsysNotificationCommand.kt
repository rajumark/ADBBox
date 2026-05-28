package com.adbstudio.desktop.adb.model.notification

import com.adbstudio.desktop.adb.model.base.AdbCommand

data class DumpsysNotificationCommand(
    override val serial: String?,
) : AdbCommand<NotificationInfo> {
    override val id: String = "shell.dumpsys.notification"

    override fun toCliArgs(): List<String> = buildList {
        if (!serial.isNullOrBlank()) {
            add("-s")
            add(serial)
        }
        addAll(listOf("shell", "dumpsys", "notification"))
    }

    override fun parse(output: String): NotificationInfo {
        val raw = output.trim()
        val notifications = mutableListOf<NotificationItem>()

        val lines = raw.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("NotificationRecord(")) {
                val notification = parseNotificationRecord(lines, i)
                if (notification != null) {
                    notifications.add(notification)
                }
            }

            i++
        }

        return NotificationInfo(raw = raw, notifications = notifications)
    }

    private fun parseNotificationRecord(lines: List<String>, startIndex: Int): NotificationItem? {
        val headerLine = lines[startIndex].trim()

        val pkg = extractValue(headerLine, "pkg=") ?: return null
        val id = extractValue(headerLine, "id=") ?: ""
        val importance = extractValue(headerLine, "importance=") ?: ""
        val key = extractValue(headerLine, "key=") ?: ""
        val channel = extractFromNotification(headerLine, "channel=")
        val userId = extractValue(headerLine, "user=") ?: ""

        var title = ""
        var text = ""
        var whenTimestamp = ""
        var color = ""
        var flags = ""
        var vis = ""

        var j = startIndex + 1
        while (j < lines.size && j < startIndex + 50) {
            val detailLine = lines[j].trim()

            if (detailLine.startsWith("NotificationRecord(") || detailLine.startsWith("  NotificationRecord(")) {
                break
            }

            if (detailLine.startsWith("flags=")) {
                flags = detailLine.substringAfter("flags=").trim()
            }

            if (detailLine.startsWith("mImportance=")) {
                val imp = detailLine.substringAfter("mImportance=").trim()
                if (imp.isNotBlank() && importance.isBlank()) {
                    // importance already captured from header
                }
            }

            if (detailLine.trimStart().startsWith("when=")) {
                val whenVal = detailLine.trimStart().substringAfter("when=").trim()
                val slashIndex = whenVal.indexOf('/')
                whenTimestamp = if (slashIndex > 0) {
                    val ms = whenVal.substring(slashIndex + 1).trim()
                    formatTimestamp(ms)
                } else {
                    formatTimestamp(whenVal)
                }
            }

            if (detailLine.trimStart().startsWith("color=")) {
                color = detailLine.trimStart().substringAfter("color=").trim()
            }

            if (detailLine.trimStart().startsWith("vis=")) {
                vis = detailLine.trimStart().substringAfter("vis=").trim()
            }

            if (detailLine.trim() == "extras={") {
                var k = j + 1
                while (k < lines.size && k < j + 15) {
                    val extrasLine = lines[k].trim()
                    if (extrasLine == "}" || extrasLine.isBlank()) break

                    if (extrasLine.startsWith("android.title=")) {
                        title = extrasLine.substringAfter("android.title=").trim()
                        title = cleanStringType(title)
                    }

                    if (extrasLine.startsWith("android.text=")) {
                        text = extrasLine.substringAfter("android.text=").trim()
                        text = cleanStringType(text)
                    }

                    k++
                }
            }

            j++
        }

        return NotificationItem(
            id = id,
            packageName = pkg,
            channelId = channel,
            importance = importance,
            title = title,
            text = text,
            timestamp = whenTimestamp,
            color = color,
            flags = flags,
            visibility = formatVisibility(vis),
            userId = userId,
            key = key,
        )
    }

    private fun extractValue(line: String, prefix: String): String? {
        val idx = line.indexOf(prefix)
        if (idx == -1) return null
        val start = idx + prefix.length
        val rest = line.substring(start)

        return when {
            rest.startsWith("{") -> {
                val end = rest.indexOf("}")
                if (end > 0) rest.substring(1, end) else ""
            }
            else -> {
                val end = rest.indexOfFirst { it == ' ' || it == ',' || it == ')' }
                if (end > 0) rest.substring(0, end) else rest.trimEnd(')')
            }
        }
    }

    private fun extractFromNotification(headerLine: String, prefix: String): String {
        val notificationStart = headerLine.indexOf("Notification(")
        if (notificationStart == -1) return ""

        val sub = headerLine.substring(notificationStart)
        val idx = sub.indexOf(prefix)
        if (idx == -1) return ""

        val start = idx + prefix.length
        val rest = sub.substring(start)
        val end = rest.indexOfFirst { it == ' ' || it == ')' || it == ',' }
        return if (end > 0) rest.substring(0, end) else rest.trimEnd(')')
    }

    private fun formatTimestamp(msStr: String): String {
        val ms = msStr.trim().toLongOrNull() ?: return msStr
        if (ms <= 0) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(ms))
        } catch (_: Exception) {
            msStr
        }
    }

    private fun cleanStringType(value: String): String {
        val prefix = "String [length="
        if (value.startsWith(prefix)) {
            return ""
        }
        val prefix2 = "String ("
        if (value.startsWith(prefix2) && value.endsWith(")")) {
            return value.substring(prefix2.length, value.length - 1)
        }
        return value
    }

    private fun formatVisibility(vis: String): String {
        return when (vis.trim()) {
            "-1" -> "UNKNOWN"
            "0" -> "PRIVATE"
            "1" -> "PUBLIC"
            "2" -> "SECRET"
            else -> vis
        }
    }
}
