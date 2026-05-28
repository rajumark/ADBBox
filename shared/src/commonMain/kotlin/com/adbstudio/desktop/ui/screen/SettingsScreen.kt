package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.ui.component.SplitView

@Composable
fun SettingsScreen() {
    SplitView(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        initialRatio = 0.5f,
        left = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Left",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
        right = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Right",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
    )
}
