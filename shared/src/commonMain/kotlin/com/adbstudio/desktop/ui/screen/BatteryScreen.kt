package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.device.DeviceRepository
import kotlinx.coroutines.launch

@Composable
fun BatteryScreen(
    adbManager: AdbManager,
    deviceRepository: DeviceRepository,
) {
    val scope = rememberCoroutineScope()
    val deviceState by deviceRepository.state.collectAsState()
    val selectedSerial = deviceState.selectedSerial

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rawOutput by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    fun refresh() {
        if (selectedSerial.isNullOrBlank()) {
            error = "No device selected"
            rawOutput = ""
            parsed = emptyList()
            return
        }
        if (!adbManager.isReady) {
            error = "ADB is not ready"
            rawOutput = ""
            parsed = emptyList()
            return
        }

        scope.launch {
            isLoading = true
            error = null
            try {
                val out = adbManager.dumpsysBattery(selectedSerial)
                rawOutput = out.trim()
                parsed = parseBatteryDump(out)
            } catch (t: Throwable) {
                error = t.message ?: "Failed to read battery info"
                rawOutput = ""
                parsed = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedSerial, adbManager.adbPath, adbManager.isReady) {
        refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Battery",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { refresh() }, enabled = !isLoading) {
                Text(text = if (isLoading) "Loading…" else "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSerial != null) {
            Text(
                text = "Device: $selectedSerial",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Parsed key/value summary
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (parsed.isEmpty()) {
                    Text(
                        text = "No data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(parsed, key = { it.first }) { (k, v) ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = k,
                                    modifier = Modifier.weight(0.45f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = v,
                                    modifier = Modifier.weight(0.55f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw output (for completeness / debugging)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Raw output (dumpsys battery)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rawOutput.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

private fun parseBatteryDump(output: String): List<Pair<String, String>> {
    // Typical format includes lines like:
    //   level: 74
    //   status: 2
    //   AC powered: false
    // We'll parse "key: value" pairs.
    return output
        .lineSequence()
        .map { it.trim() }
        .filter { it.contains(":") }
        .mapNotNull { line ->
            val idx = line.indexOf(':')
            if (idx <= 0) return@mapNotNull null
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            if (key.isBlank() || value.isBlank()) null else key to value
        }
        .distinctBy { it.first }
        .toList()
}

