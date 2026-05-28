package com.adbstudio.desktop.feature.inspector.data

import com.adbstudio.desktop.feature.inspector.domain.Rect
import com.adbstudio.desktop.feature.inspector.domain.UiNode

object WindowDumpParser {

    fun parse(xml: String): List<UiNode> {
        val nodes = mutableListOf<UiNode>()
        val lines = xml.lines()
        var nextId = 0

        fun parseLine(line: String, depth: Int) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("<node ")) return

            val attrs = extractAttributes(trimmed)
            val bounds = parseBounds(attrs["bounds"] ?: "")

            val node = UiNode(
                id = nextId++,
                index = attrs["index"] ?: "",
                text = decodeXmlEntities(attrs["text"] ?: ""),
                resourceId = attrs["resource-id"] ?: "",
                className = attrs["class"] ?: "",
                packageName = attrs["package"] ?: "",
                contentDesc = decodeXmlEntities(attrs["content-desc"] ?: ""),
                bounds = bounds,
                depth = depth,
                checkable = attrs["checkable"] == "true",
                checked = attrs["checked"] == "true",
                clickable = attrs["clickable"] == "true",
                enabled = attrs["enabled"] != "false",
                focusable = attrs["focusable"] == "true",
                focused = attrs["focused"] == "true",
                scrollable = attrs["scrollable"] == "true",
                longClickable = attrs["long-clickable"] == "true",
                password = attrs["password"] == "true",
                selected = attrs["selected"] == "true",
            )
            nodes.add(node)
        }

        var depth = 0
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("<node ") -> {
                    parseLine(trimmed, depth)
                    if (!trimmed.endsWith("/>")) {
                        depth++
                    }
                }
                trimmed.startsWith("</node>") -> {
                    depth = (depth - 1).coerceAtLeast(0)
                }
            }
        }

        return nodes
    }

    private fun extractAttributes(tag: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        // Match key="value" pairs, handling escaped quotes inside values
        val regex = Regex("""(\S+?)="((?:[^"\\]|\\.)*?)"""")
        for (match in regex.findAll(tag)) {
            map[match.groupValues[1]] = match.groupValues[2]
        }
        return map
    }

    private fun parseBounds(boundsStr: String): Rect {
        // Format: [x1,y1][x2,y2]
        val regex = Regex("""\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
        val match = regex.find(boundsStr) ?: return Rect()
        return Rect(
            left = match.groupValues[1].toInt(),
            top = match.groupValues[2].toInt(),
            right = match.groupValues[3].toInt(),
            bottom = match.groupValues[4].toInt(),
        )
    }

    private fun decodeXmlEntities(input: String): String {
        return input
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
