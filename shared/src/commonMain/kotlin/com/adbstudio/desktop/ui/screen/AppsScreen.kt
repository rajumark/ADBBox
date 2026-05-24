package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PackageInfo
import com.adbstudio.desktop.ui.component.PackageListView
import com.adbstudio.desktop.ui.component.SplitPane

@Composable
fun AppsScreen(
    packages: List<PackageInfo>,
    selectedPackage: PackageInfo?,
    onPackageSelected: (PackageInfo) -> Unit,
) {
    SplitPane(
        modifier = Modifier.fillMaxSize(),
        initialRatio = 0.35f,
        left = {
            PackageListView(
                packages = packages,
                selectedPackage = selectedPackage,
                onPackageSelected = onPackageSelected,
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            )
        },
        right = {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedPackage?.packageName ?: "App Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
