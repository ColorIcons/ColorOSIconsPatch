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
import com.immortal521.colorosiconspatch.data.CheckStatus
import com.immortal521.colorosiconspatch.data.IconSyncPlan
import com.immortal521.colorosiconspatch.data.IconSyncProgress
import com.immortal521.colorosiconspatch.data.RootImplementation
import com.immortal521.colorosiconspatch.data.XposedServiceState
import com.immortal521.colorosiconspatch.data.loadEnvironmentCheck

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
    val environment = loadEnvironmentCheck(LocalContext.current)
    val xposedService by XposedServiceState.service.collectAsState()
    val xposedStatus = if (xposedService != null) {
        CheckStatus.PASSED
    } else {
        CheckStatus.FAILED
    }
    val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "未知" }
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val kernelVersion = System.getProperty("os.version") ?: "未知"

    MainScreenScaffold(
        title = "ColorOS Icons Patch",
        contentPadding = contentPadding,
        modifier = modifier,
        actions = {
            IconButton(onClick = { showRefreshConfirmation = true }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新桌面图标")
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
                xposed = xposedStatus
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
            title = { Text("刷新桌面图标") },
            text = { Text("确定要刷新桌面图标吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showRefreshConfirmation = false
                        onRefreshLauncher()
                    }
                ) {
                    Text("刷新")
                }
            },
            dismissButton = {
                Button(onClick = { showRefreshConfirmation = false }) {
                    Text("取消")
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
    xposed: CheckStatus
) {
    val ready = root == CheckStatus.PASSED &&
        module == CheckStatus.PASSED &&
        xposed == CheckStatus.PASSED
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
        ready -> "环境正常"
        checking -> "正在检查环境"
        else -> "环境未就绪"
    }
    val summary = when {
        ready -> "${implementation.displayName} · 图标模块和 LSPosed 均已激活"
        checking -> "${implementation.displayName} · 正在确认 LSPosed 是否已激活"
        root != CheckStatus.PASSED -> "${implementation.displayName} · Root 权限不可用"
        module != CheckStatus.PASSED -> "${implementation.displayName} · 图标模块未安装"
        else -> "${implementation.displayName} · LSPosed 未激活本模块"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large
    ) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = title) },
            content = { Text(title, style = MaterialTheme.typography.titleMediumEmphasized) },
            supportingContent = {
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            },
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
                    Text("正在更新图标资源", style = MaterialTheme.typography.titleMediumEmphasized)
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
                    Text("发现 ${plan.affectedApps} 个应用有资源差异", style = MaterialTheme.typography.titleMediumEmphasized)
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
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            InfoCardItem(Icons.Filled.Tag, "Android", androidVersion)
            InfoCardItem(Icons.Filled.Security, "安全补丁", securityPatch)
            InfoCardItem(Icons.Filled.Smartphone, "设备型号", deviceModel)
            InfoCardItem(Icons.Filled.DeveloperBoard, "Kernel", kernelVersion)
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

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
