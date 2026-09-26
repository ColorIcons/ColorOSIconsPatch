package com.immortal521.colorosiconspatch.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val securityPatch = Build.VERSION.SECURITY_PATCH
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val kernelVersion = System.getProperty("os.version") ?: "未知"

    MainScreenScaffold(
        title = "ColorOS Icons Patch",
        contentPadding = contentPadding,
        modifier = modifier
    ) { contentModifier ->
        Column(
            modifier = contentModifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "系统状态",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            SystemInfoRow("Android 版本", androidVersion)
            SystemInfoRow("安全补丁", securityPatch)
            SystemInfoRow("设备型号", deviceModel)
            SystemInfoRow("Kernel 版本", kernelVersion)
            SystemInfoRow("KernelSU", "暂未检查")

            Text(
                text = "图标适配",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            HorizontalDivider()
            SystemInfoRow("已适配", "暂未读取")
            SystemInfoRow("待适配", "暂未读取")
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
