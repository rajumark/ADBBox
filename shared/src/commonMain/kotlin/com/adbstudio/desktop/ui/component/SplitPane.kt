package com.adbstudio.desktop.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.util.resizeCursor

private const val RatioMin = 0.15f
private const val RatioMax = 0.85f
private val DividerWidth = 5.dp
private val PanelMinWidth = 100.dp
private val PillWidth = 4.dp
private val PillHeight = 28.dp

@Composable
fun SplitPane(
    modifier: Modifier = Modifier,
    initialRatio: Float = 0.5f,
    onRatioChange: (Float) -> Unit = {},
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var ratio by remember { mutableFloatStateOf(initialRatio.coerceIn(RatioMin, RatioMax)) }
    var isDividerHovered by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val dividerWidthPx = with(density) { DividerWidth.toPx() }
    val minPanelPx = with(density) { PanelMinWidth.toPx() }

    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { totalWidthPx = it.width.toFloat() },
    ) {
        val availPx = (totalWidthPx - dividerWidthPx).coerceAtLeast(minPanelPx * 2 + 1f)
        val clampedMin = (minPanelPx / availPx).coerceIn(0f, RatioMax)
        val clampedMax = (1f - minPanelPx / availPx).coerceIn(RatioMin, 1f)
        val effectiveMin = clampedMin.coerceAtLeast(RatioMin)
        val effectiveMax = clampedMax.coerceAtMost(RatioMax)
        val displayRatio = ratio.coerceIn(effectiveMin, effectiveMax)
        val leftWidthDp = with(density) { (availPx * displayRatio).toDp() }

        Box(
            modifier = Modifier
                .requiredWidth(leftWidthDp)
                .fillMaxHeight(),
        ) {
            left()
        }

        Box(
            modifier = Modifier.resizeCursor(),
        ) {
            DividerHandle(
                isHovered = isDividerHovered,
                onDrag = { deltaPx ->
                    val curAvail = (totalWidthPx - dividerWidthPx).coerceAtLeast(minPanelPx * 2 + 1f)
                    val cMin = (minPanelPx / curAvail).coerceIn(0f, RatioMax)
                    val cMax = (1f - minPanelPx / curAvail).coerceIn(RatioMin, 1f)
                    val eMin = cMin.coerceAtLeast(RatioMin)
                    val eMax = cMax.coerceAtMost(RatioMax)
                    val curDisplay = ratio.coerceIn(eMin, eMax)
                    val newRatio = ((curDisplay * curAvail) + deltaPx) / curAvail
                    ratio = newRatio.coerceIn(eMin, eMax)
                    onRatioChange(ratio)
                },
                onHoverChange = { isDividerHovered = it },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            right()
        }
    }
}

@Composable
private fun DividerHandle(
    isHovered: Boolean,
    onDrag: (Float) -> Unit,
    onHoverChange: (Boolean) -> Unit,
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnHoverChange by rememberUpdatedState(onHoverChange)

    val lineColor = if (isHovered) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    }

    val pillColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .requiredWidth(DividerWidth)
            .fillMaxHeight()
            .background(lineColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> currentOnHoverChange(true)
                            PointerEventType.Exit -> currentOnHoverChange(false)
                            else -> {}
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isHovered,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(PillWidth)
                    .requiredHeight(PillHeight)
                    .background(pillColor, RoundedCornerShape(2.dp)),
            )
        }
    }
}
