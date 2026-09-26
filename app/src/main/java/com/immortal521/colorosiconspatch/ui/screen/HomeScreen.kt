package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ColorOS Icons Patch",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "系统状态",
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider()

        SystemInfoRow("Android 版本", "暂未读取")
        SystemInfoRow("设备型号", "暂未读取")
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
