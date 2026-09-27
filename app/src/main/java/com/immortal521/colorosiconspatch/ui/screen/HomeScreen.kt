package com.immortal521.colorosiconspatch.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.immortal521.colorosiconspatch.R
import com.immortal521.colorosiconspatch.data.CheckStatus
import com.immortal521.colorosiconspatch.data.IconSyncPlan
import com.immortal521.colorosiconspatch.data.IconSyncProgress
import com.immortal521.colorosiconspatch.data.RootImplementation
import com.immortal521.colorosiconspatch.data.XposedServiceState
import com.immortal521.colorosiconspatch.data.loadEnvironmentCheck
import com.immortal521.colorosiconspatch.data.checkRoot

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    plan: IconSyncPlan? = null,
    progress: IconSyncProgress? = null,
    syncing: Boolean = false,
    error: String? = null,
    onSync: () -> Unit = {},
    onRefreshLauncher: () -> Unit = {}
) {
    var showRefreshConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var environment by remember { mutableStateOf(loadEnvironmentCheck(context)) }
    LaunchedEffect(Unit) {
        if (environment.root == CheckStatus.PASSED) {
            checkRoot(context)
            environment = loadEnvironmentCheck(context)
        }
    }
    val xposedService by XposedServiceState.service.collectAsState()
    val xposedStatus = if (xposedService != null) {
        CheckStatus.PASSED
    } else {
        CheckStatus.FAILED
    }
    val androidVersion = stringResource(R.string.android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
    val securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { stringResource(R.string.unknown_value) }
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val kernelVersion = System.getProperty("os.version") ?: stringResource(R.string.unknown_value)

    MainScreenScaffold(
        title = stringResource(R.string.app_name),
        contentPadding = contentPadding,
        modifier = modifier,
        actions = {
            IconButton(onClick = { showRefreshConfirmation = true }) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh_launcher))
            }
        }
    ) { contentModifier ->
        Column(
            modifier = contentModifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            RootStatusCard(
                implementation = environment.implementation,
                root = environment.root,
                module = environment.module,
                xposed = xposedStatus,
                rootVersion = environment.rootVersion
            )

            if (plan == null || plan.totalChanges > 0 || syncing || error != null) {
                IconSyncCard(
                    plan = plan,
                    progress = progress,
                    syncing = syncing,
                    error = error,
                    onSync = onSync
                )
            }

            SystemInfoCard(
                androidVersion = androidVersion,
                securityPatch = securityPatch,
                deviceModel = deviceModel,
                kernelVersion = kernelVersion
            )
        }
    }

    if (showRefreshConfirmation) {
        AlertDialog(
            onDismissRequest = { showRefreshConfirmation = false },
            title = { Text(stringResource(R.string.refresh_icons_title)) },
            text = { Text(stringResource(R.string.refresh_launcher_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRefreshConfirmation = false
                        onRefreshLauncher()
                    }
                ) {
                    Text(stringResource(R.string.refresh))
                }
            },
            dismissButton = {
                Button(onClick = { showRefreshConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun RootStatusCard(
    implementation: RootImplementation,
    root: CheckStatus,
    module: CheckStatus,
    xposed: CheckStatus,
    rootVersion: String
) {
    val ready = root == CheckStatus.PASSED && module == CheckStatus.PASSED
    val containerColor = if (ready) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)
    val checking = root == CheckStatus.UNKNOWN ||
        module == CheckStatus.UNKNOWN ||
        xposed == CheckStatus.UNKNOWN ||
        root == CheckStatus.CHECKING ||
        module == CheckStatus.CHECKING ||
        xposed == CheckStatus.CHECKING
    val icon = when {
        ready -> Icons.Rounded.CheckCircle
        checking -> Icons.Rounded.Warning
        else -> Icons.Rounded.Block
    }
    val title = when {
        ready -> stringResource(R.string.environment_ready)
        checking -> stringResource(R.string.environment_checking)
        else -> stringResource(R.string.environment_not_ready)
    }
    val rootVersionText = rootVersion.ifBlank { stringResource(R.string.unknown_value) }
    val lsposedText = if (xposed == CheckStatus.PASSED) {
        stringResource(R.string.lsposed_active)
    } else {
        stringResource(R.string.lsposed_inactive_refresh)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(icon, contentDescription = title)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    stringResource(R.string.root_manager_version, implementation.displayName, rootVersionText),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    lsposedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (xposed == CheckStatus.PASSED) contentColor else MaterialTheme.colorScheme.error
                )
            }
            StatusPill(
                text = stringResource(if (ready) R.string.status_ready else R.string.status_needs_action),
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun IconSyncCard(
    plan: IconSyncPlan?,
    progress: IconSyncProgress?,
    syncing: Boolean,
    error: String?,
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.contentColorFor(
                MaterialTheme.colorScheme.tertiaryContainer
            )
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            when {
                syncing && progress != null -> {
                    Text(stringResource(R.string.updating_icons), style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(progress.currentApp ?: stringResource(R.string.preparing), style = MaterialTheme.typography.bodyMedium)
                    progress.currentFile?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (progress.totalApps == 0) 1f else {
                                progress.completedApps.toFloat() / progress.totalApps
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.app_progress, progress.completedApps, progress.totalApps),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                plan == null -> Text(stringResource(R.string.checking_icon_changes))
                else -> {
                    Text(stringResource(R.string.icon_changes_found, plan.affectedApps), style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(
                        stringResource(R.string.files_to_update, plan.updates.size, plan.staleFiles.size, formatBytes(plan.totalBytes)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onSync, enabled = !syncing) {
                        Text(stringResource(R.string.download_update))
                    }
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SystemInfoCard(
    androidVersion: String,
    securityPatch: String,
    deviceModel: String,
    kernelVersion: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            InfoCardItem(Icons.Filled.Android, stringResource(R.string.android_label), androidVersion)
            InfoCardItem(Icons.Filled.Security, stringResource(R.string.security_patch), securityPatch)
            InfoCardItem(Icons.Filled.Smartphone, stringResource(R.string.device_model), deviceModel)
            InfoCardItem(Icons.Filled.DeveloperBoard, stringResource(R.string.kernel_label), kernelVersion)
        }
    }
}

@Composable
private fun InfoCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> stringResource(R.string.bytes, bytes)
    bytes < 1024 * 1024 -> stringResource(R.string.kilobytes, bytes / 1024f)
    else -> stringResource(R.string.megabytes, bytes / (1024f * 1024f))
}
