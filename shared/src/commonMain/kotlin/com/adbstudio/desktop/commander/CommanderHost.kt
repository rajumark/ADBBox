package com.adbstudio.desktop.commander

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun CommanderHost(
    registry: CommanderRegistry = remember { CommanderRegistry() },
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalCommanderRegistry provides registry) {
            content()
        }
    }
}
