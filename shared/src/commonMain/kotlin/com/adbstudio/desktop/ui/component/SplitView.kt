package com.adbstudio.desktop.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SplitView(
    modifier: Modifier = Modifier,
    initialRatio: Float = 0.5f,
    dividerWidth: Dp = 6.dp,
    minLeftWidth: Dp = 100.dp,
    minRightWidth: Dp = 100.dp,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    var splitRatio by remember { mutableStateOf(initialRatio) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidthDp = maxWidth
        val totalWidthPx = constraints.maxWidth.toFloat()
        val dividerWidthPx = with(LocalDensity.current) { dividerWidth.toPx() }
        val minLeftWidthPx = with(LocalDensity.current) { minLeftWidth.toPx() }
        val minRightWidthPx = with(LocalDensity.current) { minRightWidth.toPx() }
        val maxLeftWidthPx = totalWidthPx - dividerWidthPx - minRightWidthPx

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalWidthDp * splitRatio),
            ) {
                left()
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dividerWidth)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {},
                            onDragEnd = {},
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val currentLeftWidthPx = totalWidthPx * splitRatio
                                val newLeftWidthPx = (currentLeftWidthPx + dragAmount)
                                    .coerceIn(minLeftWidthPx, maxLeftWidthPx)
                                splitRatio = newLeftWidthPx / totalWidthPx
                            },
                        )
                    },
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalWidthDp - totalWidthDp * splitRatio - dividerWidth),
            ) {
                right()
            }
        }
    }
}
