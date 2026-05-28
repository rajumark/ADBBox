package com.adbstudio.desktop.feature.inspector.presentation

import com.adbstudio.desktop.feature.inspector.domain.UiNode

data class UiInspectorUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activityName: String? = null,
    val fragmentInfo: String? = null,
    val xmlContent: String? = null,
    val screenshotBytes: ByteArray? = null,
    val nodes: List<UiNode> = emptyList(),
    val selectedNodeId: Int? = null,
    val layerDepth: Int = -1, // -1 = all layers
    val nodeTraversalIndex: Int = -1, // -1 = none
) {
    val hasContent: Boolean
        get() = xmlContent != null || screenshotBytes != null

    val maxDepth: Int
        get() = nodes.maxOfOrNull { it.depth } ?: 0

    val visibleNodes: List<UiNode>
        get() = if (layerDepth < 0) nodes else nodes.filter { it.depth == layerDepth }

    val selectedNode: UiNode?
        get() = nodes.find { it.id == selectedNodeId }
            ?: if (nodeTraversalIndex in nodes.indices) nodes[nodeTraversalIndex] else null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UiInspectorUiState

        if (selectedSerial != other.selectedSerial) return false
        if (isLoading != other.isLoading) return false
        if (errorMessage != other.errorMessage) return false
        if (activityName != other.activityName) return false
        if (fragmentInfo != other.fragmentInfo) return false
        if (xmlContent != other.xmlContent) return false
        if (screenshotBytes != null) {
            if (other.screenshotBytes == null) return false
            if (!screenshotBytes.contentEquals(other.screenshotBytes)) return false
        } else if (other.screenshotBytes != null) return false
        if (nodes != other.nodes) return false
        if (selectedNodeId != other.selectedNodeId) return false
        if (layerDepth != other.layerDepth) return false
        if (nodeTraversalIndex != other.nodeTraversalIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedSerial?.hashCode() ?: 0
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (activityName?.hashCode() ?: 0)
        result = 31 * result + (fragmentInfo?.hashCode() ?: 0)
        result = 31 * result + (xmlContent?.hashCode() ?: 0)
        result = 31 * result + (screenshotBytes?.contentHashCode() ?: 0)
        result = 31 * result + nodes.hashCode()
        result = 31 * result + (selectedNodeId ?: 0)
        result = 31 * result + layerDepth
        result = 31 * result + nodeTraversalIndex
        return result
    }
}
