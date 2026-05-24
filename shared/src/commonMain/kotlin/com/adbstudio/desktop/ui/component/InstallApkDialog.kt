package com.adbstudio.desktop.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InstallApkDialog(
    state: InstallState,
    onDismiss: () -> Unit,
    onCopyError: (String) -> Unit,
) {
    when (state) {
        is InstallState.Idle -> {}
        is InstallState.Installing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Installing APK", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Column {
                        Text(
                            "Installing ${state.fileName}…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {},
            )
        }
        is InstallState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text("Install Successful", style = MaterialTheme.typography.titleSmall)
                },
                text = {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                },
                confirmButton = {
                    Button(onClick = onDismiss) { Text("Close") }
                },
            )
        }
        is InstallState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Install Failed", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Column {
                        Text(
                            "Installation failed:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        ErrorLogView(state.summary, state.fullLog, onCopyError)
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) { Text("Close") }
                },
            )
        }
    }
}

@Composable
private fun ErrorLogView(
    summary: String,
    fullLog: String,
    onCopy: (String) -> Unit,
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll),
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = fullLog,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    TextButton(onClick = { onCopy(fullLog) }) {
        Text("Copy Error Log", style = MaterialTheme.typography.labelMedium)
    }
}
