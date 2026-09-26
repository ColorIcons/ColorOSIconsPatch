package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    MainScreenScaffold(
        title = "应用",
        contentPadding = contentPadding,
        modifier = modifier
    ) { contentModifier ->
        PlaceholderContent(
            modifier = contentModifier,
            message = "已安装应用将在这里显示"
        )
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    MainScreenScaffold(
        title = "设置",
        contentPadding = contentPadding,
        modifier = modifier
    ) { contentModifier ->
        PlaceholderContent(
            modifier = contentModifier,
            message = "应用设置将在这里显示"
        )
    }
}

@Composable
private fun PlaceholderContent(
    modifier: Modifier,
    message: String
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
