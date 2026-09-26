package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        title = "应用",
        message = "已安装应用将在这里显示"
    )
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        title = "设置",
        message = "应用设置将在这里显示"
    )
}

@Composable
private fun PlaceholderScreen(
    modifier: Modifier,
    title: String,
    message: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message)
    }
}
