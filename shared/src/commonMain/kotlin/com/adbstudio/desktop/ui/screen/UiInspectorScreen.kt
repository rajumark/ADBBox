@file:OptIn(ExperimentalMaterial3Api::class)

package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.feature.inspector.domain.UiNode
import com.adbstudio.desktop.feature.inspector.presentation.UiInspectorEvent
import com.adbstudio.desktop.feature.inspector.presentation.UiInspectorViewModel
import com.adbstudio.desktop.platform.toImageBitmapOrNull
import com.adbstudio.desktop.ui.component.SplitView
import kotlin.math.min

@Composable
fun UiInspectorScreen(
    viewModel: UiInspectorViewModel,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "UI Inspector",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(24.dp).height(24.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Button(
                onClick = { viewModel.onEvent(UiInspectorEvent.Refresh) },
                enabled = !state.isLoading && !state.selectedSerial.isNullOrBlank(),
            ) {
                Text(text = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Activity / Fragment info
        if (state.activityName != null || state.fragmentInfo != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (state.activityName != null) {
                        Text(
                            text = "Activity: ${state.activityName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.fragmentInfo != null) {
                        Text(
                            text = "Fragment: ${state.fragmentInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.selectedSerial != null) {
            Text(
                text = "Device: ${state.selectedSerial}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Controls when content exists
        if (state.nodes.isNotEmpty()) {
            ControlsRow(
                maxDepth = state.maxDepth,
                layerDepth = state.layerDepth,
                nodeCount = state.nodes.size,
                nodeTraversalIndex = state.nodeTraversalIndex,
                onLayerDepthChange = { viewModel.onEvent(UiInspectorEvent.SetLayerDepth(it)) },
                onNodeTraversalChange = { viewModel.onEvent(UiInspectorEvent.SetNodeTraversalIndex(it)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!state.hasContent && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.errorMessage != null) "" else "Click Refresh to capture device UI",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SplitView(
                modifier = Modifier.fillMaxSize(),
                initialRatio = 0.35f,
                left = {
                    XmlTreePanel(
                        nodes = state.nodes,
                        selectedNodeId = state.selectedNodeId,
                        nodeTraversalIndex = state.nodeTraversalIndex,
                        onNodeClick = { viewModel.onEvent(UiInspectorEvent.SelectNode(it)) },
                    )
                },
                right = {
                    ScreenshotOverlayPanel(
                        screenshotBytes = state.screenshotBytes,
                        nodes = state.visibleNodes,
                        selectedNodeId = state.selectedNodeId,
                        onNodeTap = { viewModel.onEvent(UiInspectorEvent.SelectNode(it)) },
                    )
                },
            )
        }
    }
}

@Composable
private fun ControlsRow(
    maxDepth: Int,
    layerDepth: Int,
    nodeCount: Int,
    nodeTraversalIndex: Int,
    onLayerDepthChange: (Int) -> Unit,
    onNodeTraversalChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (maxDepth >= 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Layer:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(56.dp),
                )
                Text(
                    text = if (layerDepth < 0) "All" else "$layerDepth",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(32.dp),
                )
                Slider(
                    value = (layerDepth + 1).toFloat(),
                    onValueChange = { onLayerDepthChange(it.toInt() - 1) },
                    valueRange = 0f..(maxDepth + 1).toFloat(),
                    steps = maxDepth + 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (nodeCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Node:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(56.dp),
                )
                Text(
                    text = if (nodeTraversalIndex < 0) "—" else "${nodeTraversalIndex + 1}/$nodeCount",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(56.dp),
                )
                Slider(
                    value = (nodeTraversalIndex + 1).toFloat(),
                    onValueChange = { onNodeTraversalChange(it.toInt() - 1) },
                    valueRange = 0f..nodeCount.toFloat(),
                    steps = nodeCount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun XmlTreePanel(
    nodes: List<UiNode>,
    selectedNodeId: Int?,
    nodeTraversalIndex: Int,
    onNodeClick: (Int?) -> Unit,
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        if (nodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No XML data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
                    .padding(4.dp),
            ) {
                items(nodes, key = { it.id }) { node ->
                    val isSelected = node.id == selectedNodeId
                            || (nodeTraversalIndex >= 0 && nodes.getOrNull(nodeTraversalIndex)?.id == node.id)
                    NodeRow(
                        node = node,
                        isSelected = isSelected,
                        onClick = { onNodeClick(if (isSelected) null else node.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeRow(
    node: UiNode,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 12).dp)
            .background(bg, shape = RoundedCornerShape(4.dp))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = node.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "[${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}]",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ScreenshotOverlayPanel(
    screenshotBytes: ByteArray?,
    nodes: List<UiNode>,
    selectedNodeId: Int?,
    onNodeTap: (Int?) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        if (screenshotBytes == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No screenshot",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val imageBitmap = remember(screenshotBytes) {
                screenshotBytes.toImageBitmapOrNull()
            }
            if (imageBitmap != null) {
                val density = LocalDensity.current
                val imageWidth = imageBitmap.width.toFloat()
                val imageHeight = imageBitmap.height.toFloat()

                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                        .horizontalScroll(horizontalScroll),
                    contentAlignment = Alignment.Center,
                ) {
                    val boxWidthPx = with(density) { maxWidth.toPx() }
                    val boxHeightPx = with(density) { maxHeight.toPx() }

                    val scale = min(
                        boxWidthPx / imageWidth,
                        boxHeightPx / imageHeight,
                    ).coerceAtMost(1f) // Don't upscale beyond native resolution

                    val drawWidth = imageWidth * scale
                    val drawHeight = imageHeight * scale

                    Box(
                        modifier = Modifier
                            .size(
                                with(density) { drawWidth.toDp() },
                                with(density) { drawHeight.toDp() },
                            )
                            .pointerInput(nodes, scale, imageWidth, imageHeight) {
                                detectTapGestures { offset ->
                                    val imgX = (offset.x / scale).toInt()
                                    val imgY = (offset.y / scale).toInt()
                                    val tappedNode = findNodeAtPoint(nodes, imgX, imgY)
                                    onNodeTap(tappedNode?.id)
                                }
                            },
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Device screenshot",
                            modifier = Modifier.fillMaxSize(),
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            nodes.forEach { node ->
                                val isSelected = node.id == selectedNodeId
                                val color = if (isSelected) Color.Red else Color.Cyan
                                val strokeWidth = if (isSelected) 3f else 1.5f
                                val alpha = if (isSelected) 1f else 0.5f

                                drawRect(
                                    color = color.copy(alpha = alpha),
                                    topLeft = Offset(
                                        x = node.bounds.left * scale,
                                        y = node.bounds.top * scale,
                                    ),
                                    size = Size(
                                        width = node.bounds.width * scale,
                                        height = node.bounds.height * scale,
                                    ),
                                    style = Stroke(width = strokeWidth),
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Failed to decode screenshot",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun findNodeAtPoint(nodes: List<UiNode>, x: Int, y: Int): UiNode? {
    // Find all nodes containing the point, then pick the smallest (most specific) one
    val candidates = nodes.filter { it.bounds.contains(x, y) && !it.bounds.isEmpty() }
    return candidates.minByOrNull { it.area }
}
