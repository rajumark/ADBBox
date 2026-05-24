package com.adbstudio.desktop.ui.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PackageContextAction
import com.adbstudio.desktop.device.PackageInfo

@Composable
fun AppDetailsContent(
    pkg: PackageInfo,
    showBack: Boolean,
    onBack: () -> Unit,
    onAction: (PackageContextAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (showBack) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "App Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = pkg.packageName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(32.dp))

        ActionSection(
            title = "Actions",
            actions = listOf(
                "Open" to PackageContextAction.Open,
                "Force Stop" to PackageContextAction.ForceStop,
                "Restart" to PackageContextAction.Restart,
            ),
            onAction = onAction,
        )

        Spacer(Modifier.height(16.dp))

        ActionSection(
            title = "Manage",
            actions = listOf(
                "Uninstall" to PackageContextAction.Uninstall,
                "Clear Data" to PackageContextAction.ClearData,
                "Enable" to PackageContextAction.Enable,
                "Disable" to PackageContextAction.Disable,
            ),
            onAction = onAction,
        )

        Spacer(Modifier.height(16.dp))

        ActionSection(
            title = "Info",
            actions = listOf(
                "Open App Info" to PackageContextAction.OpenAppInfo,
                "View at Playstore" to PackageContextAction.ViewAtPlaystore,
            ),
            onAction = onAction,
        )
    }
}

@Composable
private fun ActionSection(
    title: String,
    actions: List<Pair<String, PackageContextAction>>,
    onAction: (PackageContextAction) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { (label, action) ->
                Button(
                    onClick = { onAction(action) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
