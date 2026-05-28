package com.adbstudio.desktop.feature.inspector.presentation

sealed interface UiInspectorEvent {
    data object Refresh : UiInspectorEvent
    data class SelectNode(val nodeId: Int?) : UiInspectorEvent
    data class SetLayerDepth(val depth: Int) : UiInspectorEvent
    data class SetNodeTraversalIndex(val index: Int) : UiInspectorEvent
}
