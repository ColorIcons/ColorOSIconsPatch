package com.immortal521.colorosiconspatch.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

private data class NavigationDestination(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val destinations = listOf(
        NavigationDestination("首页", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationDestination("应用", Icons.Filled.Apps, Icons.Outlined.Apps),
        NavigationDestination("设置", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationBar {
        destinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == index) {
                            destination.selectedIcon
                        } else {
                            destination.unselectedIcon
                        },
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
