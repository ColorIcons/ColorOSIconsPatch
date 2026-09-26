package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.InstalledApp

@Composable
fun AppsScreen(
    apps: List<InstalledApp>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    MainScreenScaffold(
        title = "应用",
        contentPadding = contentPadding,
        modifier = modifier,
        scrollable = false
    ) { contentModifier ->
        when (val appList = apps) {
            null -> Text(
                text = "正在加载应用…",
                modifier = contentModifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(appList, key = { it.packageName }) { app ->
                    AppRow(app)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (app.isAdapted) "已适配" else "未适配",
            style = MaterialTheme.typography.labelMedium,
            color = if (app.isAdapted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
