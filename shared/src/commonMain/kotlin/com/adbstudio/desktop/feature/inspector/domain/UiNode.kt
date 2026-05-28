package com.adbstudio.desktop.feature.inspector.domain

/**
 * Represents a parsed UI node from a uiautomator dump.
 */
data class UiNode(
    val id: Int,
    val index: String = "",
    val text: String = "",
    val resourceId: String = "",
    val className: String = "",
    val packageName: String = "",
    val contentDesc: String = "",
    val bounds: Rect = Rect(),
    val depth: Int = 0,
    val checkable: Boolean = false,
    val checked: Boolean = false,
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val focusable: Boolean = false,
    val focused: Boolean = false,
    val scrollable: Boolean = false,
    val longClickable: Boolean = false,
    val password: Boolean = false,
    val selected: Boolean = false,
) {
    val area: Int
        get() = bounds.width * bounds.height

    val displayLabel: String
        get() = buildString {
            append(className.substringAfterLast('.'))
            if (text.isNotBlank()) {
                append(" • \"$text\"")
            } else if (contentDesc.isNotBlank()) {
                append(" • [$contentDesc]")
            } else if (resourceId.isNotBlank()) {
                append(" • @id/$resourceId")
            }
        }
}

data class Rect(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun contains(x: Int, y: Int): Boolean =
        x >= left && x <= right && y >= top && y <= bottom

    fun isEmpty(): Boolean = width <= 0 || height <= 0
}
