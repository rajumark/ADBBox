@file:OptIn(ExperimentalMaterial3Api::class)

package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.feature.messages.presentation.MessagesEvent
import com.adbstudio.desktop.feature.messages.presentation.MessagesViewModel

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilterChip(
                selected = state.showOriginal,
                onClick = { viewModel.onEvent(MessagesEvent.ToggleOriginal) },
                label = { Text("Original") },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { viewModel.onEvent(MessagesEvent.Refresh) },
                enabled = !state.isLoading,
            ) {
                Text(text = if (state.isLoading) "Loading…" else "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.selectedSerial != null) {
            Text(
                text = "Device: ${state.selectedSerial}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onEvent(MessagesEvent.SetSearchQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search in ${state.data.size} items…") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.data.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.errorMessage != null) "" else "No messages found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            MessagesTable(
                columns = state.columns,
                rows = state.filteredData,
            )
        }
    }
}

@Composable
private fun MessagesTable(
    columns: List<String>,
    rows: List<Map<String, String>>,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val horizontalScrollModifier = Modifier.horizontalScroll(scrollState)

            Row(
                modifier = horizontalScrollModifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                columns.forEach { column ->
                    Text(
                        text = column,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.width(messagesColumnWidth(column)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
            ) {
                itemsIndexed(rows) { index, row ->
                    val bg = if (index % 2 == 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant
                    Row(
                        modifier = Modifier
                            .background(bg)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        columns.forEach { column ->
                            Text(
                                text = row[column] ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                modifier = Modifier.width(messagesColumnWidth(column)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun messagesColumnWidth(column: String): Dp {
    val charWidth = 8.dp
    val maxChars = when (column) {
        "_id" -> 8
        "thread_id" -> 8
        "address" -> 20
        "person" -> 10
        "date", "date_sent" -> 20
        "protocol" -> 10
        "read", "seen" -> 8
        "status" -> 10
        "type" -> 16
        "reply_path_present" -> 14
        "subject" -> 20
        "body" -> 50
        "service_center" -> 20
        "locked" -> 8
        "error_code" -> 10
        else -> 20
    }
    return charWidth * maxChars
}
