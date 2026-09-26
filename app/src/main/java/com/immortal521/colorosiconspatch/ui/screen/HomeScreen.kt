package com.immortal521.colorosiconspatch.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.CheckStatus
import com.immortal521.colorosiconspatch.data.IconSyncPlan
import com.immortal521.colorosiconspatch.data.IconSyncProgress
import com.immortal521.colorosiconspatch.data.RootImplementation
import com.immortal521.colorosiconspatch.data.loadEnvironmentCheck

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    plan: IconSyncPlan? = null,
    progress: IconSyncProgress? = null,
    syncing: Boolean = false,
    error: String? = null,
    onSync: () -> Unit = {}
) {
    val environment = loadEnvironmentCheck(LocalContext.current)
    val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "未知" }
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val kernelVersion = System.getProperty("os.version") ?: "未知"

    MainScreenScaffold(
        title = "ColorOS Icons Patch",
        contentPadding = contentPadding,
        modifier = modifier
    ) { contentModifier ->
        Column(
            modifier = contentModifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            RootStatusCard(
                implementation = environment.implementation,
                root = environment.root,
                module = environment.module
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
}

@Composable
private fun RootStatusCard(
    implementation: RootImplementation,
    root: CheckStatus,
    module: CheckStatus
) {
    val ready = root == CheckStatus.PASSED && module == CheckStatus.PASSED
    val containerColor = if (ready) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)
    val icon = when {
        ready -> Icons.Rounded.CheckCircle
        root == CheckStatus.UNKNOWN || module == CheckStatus.UNKNOWN -> Icons.Rounded.Warning
        else -> Icons.Rounded.Block
    }
    val title = when {
        ready -> "环境正常"
        root == CheckStatus.UNKNOWN || module == CheckStatus.UNKNOWN -> "正在检查环境"
        else -> "环境未就绪"
    }
    val summary = when {
        ready -> "${implementation.displayName} · 图标模块已加载"
        root != CheckStatus.PASSED -> "${implementation.displayName} · Root 权限不可用"
        else -> "${implementation.displayName} · 图标模块未安装"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large
    ) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = title) },
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(summary) },
            trailingContent = {
                StatusPill(
                    text = if (ready) "已就绪" else "需处理",
                    color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = contentColor,
                leadingIconColor = contentColor,
                trailingIconColor = contentColor,
                supportingColor = contentColor.copy(alpha = 0.75f)
            )
        )
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
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            when {
                syncing && progress != null -> {
                    Text("正在更新图标资源", style = MaterialTheme.typography.titleMedium)
                    Text(progress.currentApp ?: "准备中", style = MaterialTheme.typography.bodyMedium)
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
                        "应用 ${progress.completedApps} / ${progress.totalApps}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                plan == null -> Text("正在检查图标资源差异…")
                else -> {
                    Text("发现 ${plan.affectedApps} 个应用有资源差异", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${plan.updates.size} 个文件需要下载，${plan.staleFiles.size} 个文件需要删除 · ${formatBytes(plan.totalBytes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onSync, enabled = !syncing) {
                        Text("下载并更新")
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
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            InfoCardItem(
                icon = Icons.Filled.Tag,
                label = "Android",
                content = androidVersion
            )
            InfoCardItem(
                icon = Icons.Filled.Security,
                label = "安全补丁",
                content = securityPatch
            )
            InfoCardItem(
                icon = Icons.Filled.Smartphone,
                label = "设备型号",
                content = deviceModel
            )
            InfoCardItem(
                icon = Icons.Filled.DeveloperBoard,
                label = "Kernel",
                content = kernelVersion
            )
        }
    }
}

@Composable
private fun InfoCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: String
) {
    ListItem(
        leadingContent = {
            Icon(imageVector = icon, contentDescription = label)
        },
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
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

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
