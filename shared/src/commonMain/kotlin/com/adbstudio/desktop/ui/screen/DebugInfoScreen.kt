package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.util.FileEntry
import com.adbstudio.desktop.util.listDirectoryTree

@Composable
fun DebugInfoScreen(adbManager: AdbManager) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val treeEntries = remember(adbManager.appDataDir) {
        listDirectoryTree(adbManager.appDataDir)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Debug Info",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        InfoCard(
            title = "ADB Path",
            value = adbManager.adbPath,
            onCopy = {
                clipboardManager.setText(AnnotatedString(adbManager.adbPath))
                copied = true
            },
            copied = copied,
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(
            title = "ADB Status",
            value = if (adbManager.isReady) "Ready" else "Not Ready",
            valueColor = if (adbManager.isReady) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "App Directory",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = adbManager.appDataDir,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    treeEntries.forEach { entry ->
                        FileTreeRow(entry, depth = 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    onCopy: (() -> Unit)? = null,
    copied: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (onCopy != null) {
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onCopy,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Text(text = if (copied) "Copied!" else "Copy")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTreeRow(entry: FileEntry, depth: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (entry.depth * 20).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (entry.isDirectory) "📁 " else "📄 ",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = if (entry.isDirectory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!entry.isDirectory) {
            Text(
                text = "  (${formatSize(entry.size)})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
    entry.children.forEach { child ->
        FileTreeRow(child, depth + 1)
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    }
}
