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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.feature.processes.presentation.ProcessesEvent
import com.adbstudio.desktop.feature.processes.presentation.ProcessesViewModel

@Composable
fun ProcessesScreen(
    viewModel: ProcessesViewModel,
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
                text = "Processes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilterChip(
                selected = state.showAll,
                onClick = { viewModel.onEvent(ProcessesEvent.ToggleShowAll) },
                label = { Text("Show all") },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { viewModel.onEvent(ProcessesEvent.Refresh) },
                enabled = !state.isLoading,
            ) {
                Text(text = if (state.isLoading) "Loading\u2026" else "Refresh")
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
            onValueChange = { viewModel.onEvent(ProcessesEvent.SetSearchQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search in ${state.data.size} items\u2026") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.data.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.errorMessage != null) "" else "No data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ProcessesTable(
                columns = state.columns,
                rows = state.filteredData,
            )
        }
    }
}

@Composable
private fun ProcessesTable(
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
                        modifier = Modifier.width(columnWidth(column)),
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
                                modifier = Modifier.width(columnWidth(column)),
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
private fun columnWidth(column: String): Dp {
    val density = LocalDensity.current
    val charWidth = with(density) { 8.dp.toPx() }.toInt()
    val maxChars = when (column) {
        "USER" -> 16
        "PID" -> 8
        "PPID" -> 8
        "VSZ" -> 10
        "RSS" -> 8
        "WCHAN" -> 22
        "ADDR" -> 10
        "S" -> 4
        "NAME" -> 50
        else -> 16
    }
    return with(density) { (maxChars * charWidth * 0.6f).toDp().coerceAtLeast(80.dp) }
}
