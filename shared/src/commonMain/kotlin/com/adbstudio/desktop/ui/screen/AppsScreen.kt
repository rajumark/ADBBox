package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.ui.component.SplitView
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(
    adbManager: AdbManager,
    deviceRepository: DeviceRepository,
) {
    val deviceState = deviceRepository.state.collectAsState().value
    val selectedSerial = deviceState.selectedSerial
    val scope = rememberCoroutineScope()

    // Simple local state for now (can move to a feature ViewModel later).
    var packages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadPackages() {
        if (selectedSerial.isNullOrBlank()) {
            packages = emptyList()
            error = "No device selected"
            return
        }
        if (!adbManager.isReady) {
            packages = emptyList()
            error = "ADB is not ready"
            return
        }

        isLoading = true
        error = null
        try {
            val result = adbManager.listPackages(selectedSerial).sorted()
            if (result != packages) packages = result
        } catch (t: Throwable) {
            packages = emptyList()
            error = t.message ?: "Failed to load packages"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedSerial, adbManager.adbPath, adbManager.isReady) {
        loadPackages()
    }

    SplitView(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        initialRatio = 0.4f,
        left = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Packages",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(12.dp),
                )

                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                Button(
                    onClick = { scope.launch { loadPackages() } },
                    enabled = !isLoading,
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(text = if (isLoading) "Loading…" else "Refresh")
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = packages, key = { it }) { pkg ->
                        ListItem(headlineContent = { Text(pkg) })
                    }
                }
            }
        },
        right = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Select a package to see details (coming soon)",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        },
    )
}
