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
import com.adbstudio.desktop.feature.media.domain.MediaSourceType
import com.adbstudio.desktop.feature.media.domain.MediaContentType
import com.adbstudio.desktop.feature.media.presentation.MediaEvent
import com.adbstudio.desktop.feature.media.presentation.MediaViewModel

@Composable
fun MediaScreen(
    viewModel: MediaViewModel,
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
                text = "Media",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            MediaSourceType.entries.forEach { source ->
                FilterChip(
                    selected = state.sourceType == source,
                    onClick = { viewModel.onEvent(MediaEvent.SetSourceType(source)) },
                    label = { Text(source.title) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            MediaContentType.entries.forEach { content ->
                FilterChip(
                    selected = state.contentType == content,
                    onClick = { viewModel.onEvent(MediaEvent.SetContentType(content)) },
                    label = { Text(content.title) },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            FilterChip(
                selected = state.showOriginal,
                onClick = { viewModel.onEvent(MediaEvent.ToggleOriginal) },
                label = { Text("Original") },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { viewModel.onEvent(MediaEvent.Refresh) },
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
            onValueChange = { viewModel.onEvent(MediaEvent.SetSearchQuery(it)) },
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
                    text = if (state.errorMessage != null) "" else "No media found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            MediaTable(
                columns = getMediaColumns(state.data),
                rows = filterMediaRows(state.data, state.searchQuery),
            )
        }
    }
}

@Composable
private fun MediaTable(
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
                        modifier = Modifier.width(mediaColumnWidth(column)),
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
                                modifier = Modifier.width(mediaColumnWidth(column)),
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
private fun mediaColumnWidth(column: String): Dp {
    val charWidth = 8.dp
    val maxChars = when (column) {
        "_id" -> 8
        "_display_name" -> 30
        "_size" -> 15
        "mime_type" -> 20
        "date_added", "date_modified", "datetaken" -> 20
        "width", "height" -> 8
        "duration" -> 10
        else -> 25
    }
    return charWidth * maxChars
}

private fun getMediaColumns(data: List<Map<String, String>>): List<String> {
    if (data.isEmpty()) return emptyList()
    
    val columnOrder = listOf(
        "_id", "_display_name", "_size", "mime_type",
        "date_added", "date_modified", "datetaken",
        "width", "height", "duration"
    )
    
    val allKeys = data.flatMap { it.keys }.toSet()
    val orderedKeys = columnOrder.filter { it in allKeys }
    val remainingKeys = allKeys.sorted().filter { it !in columnOrder }
    
    return orderedKeys + remainingKeys
}

private fun filterMediaRows(data: List<Map<String, String>>, query: String): List<Map<String, String>> {
    if (query.isBlank()) return data
    
    val lowerQuery = query.lowercase()
    return data.filter { row ->
        row.any { (_, value) ->
            value?.lowercase()?.contains(lowerQuery) ?: false
        }
    }
}
