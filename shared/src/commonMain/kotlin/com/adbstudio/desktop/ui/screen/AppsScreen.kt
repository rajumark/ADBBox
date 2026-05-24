package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.adb.AdbManager

@Composable
fun AppsScreen(adbManager: AdbManager) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ADB Path",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = adbManager.adbPath,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (adbManager.isReady) "Status: Ready" else "Status: Not Ready",
            style = MaterialTheme.typography.bodyMedium,
            color = if (adbManager.isReady) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
        )
    }
}
