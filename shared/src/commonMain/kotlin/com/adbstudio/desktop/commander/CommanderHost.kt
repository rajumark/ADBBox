package com.adbstudio.desktop.commander

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlin.time.TimeSource

@Composable
fun CommanderHost(
    registry: CommanderRegistry = remember { CommanderRegistry() },
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    var lastShiftTime by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val doubleShiftThresholdMs = 400.0

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.ShiftLeft || event.key == Key.ShiftRight)
                ) {
                    val now = TimeSource.Monotonic.markNow()
                    val elapsed = (now - lastShiftTime).inWholeMilliseconds
                    lastShiftTime = now
                    if (elapsed < doubleShiftThresholdMs) {
                        isOpen = !isOpen
                    }
                    true
                } else {
                    false
                }
            },
    ) {
        CompositionLocalProvider(LocalCommanderRegistry provides registry) {
            content()
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CommanderDialog(
                registry = registry,
                onDismiss = { isOpen = false },
                onActionSelected = { action ->
                    action.action()
                    isOpen = false
                },
            )
        }
    }
}
