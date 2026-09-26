package com.immortal521.colorosiconspatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.InstalledApp
import com.immortal521.colorosiconspatch.data.loadIconIndex
import com.immortal521.colorosiconspatch.data.syncIconResources
import com.immortal521.colorosiconspatch.data.loadInstalledApps
import com.immortal521.colorosiconspatch.ui.navigation.MainNavigationBar
import com.immortal521.colorosiconspatch.ui.onboarding.InitializationFlow
import com.immortal521.colorosiconspatch.ui.screen.AppsScreen
import com.immortal521.colorosiconspatch.ui.screen.HomeScreen
import com.immortal521.colorosiconspatch.ui.screen.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App() {
    InitializationFlow {
        MainContent()
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedTab) { 3 }
    val pagerScope = rememberCoroutineScope()
    var installedApps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var iconIndexError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == 1 && installedApps == null) {
            val index = withContext(Dispatchers.IO) {
                loadIconIndex(context)
            }
            iconIndexError = index.error
            val apps = withContext(Dispatchers.IO) {
                loadInstalledApps(context, index.adaptedPackages)
            }
            installedApps = apps
            launch {
                withContext(Dispatchers.IO) {
                    syncIconResources(
                        context = context,
                        installedPackages = apps.mapTo(mutableSetOf()) { it.packageName },
                        index = index
                    )
                }
            }
        }
    }

    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
            .collect { selectedTab = it }
    }

    BackHandler(enabled = selectedTab != 0) {
        pagerScope.launch { pagerState.animateScrollToPage(0) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    pagerScope.launch { pagerState.animateScrollToPage(it) }
                }
            )
        }
    ) { innerPadding ->
        val mainContentPadding = PaddingValues(
            bottom = innerPadding.calculateBottomPadding()
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) { page ->
            when (page) {
                0 -> HomeScreen(contentPadding = mainContentPadding)
                1 -> AppsScreen(
                    apps = installedApps,
                    indexError = iconIndexError,
                    contentPadding = mainContentPadding
                )
                2 -> SettingsScreen(contentPadding = mainContentPadding)
            }
        }
    }
}
