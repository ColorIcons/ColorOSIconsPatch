package com.immortal521.colorosiconspatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.immortal521.colorosiconspatch.data.IconIndexLoadResult
import com.immortal521.colorosiconspatch.data.IconSyncProgress
import com.immortal521.colorosiconspatch.data.IconSyncPlan
import com.immortal521.colorosiconspatch.data.InstalledApp
import com.immortal521.colorosiconspatch.data.buildIconSyncPlan
import com.immortal521.colorosiconspatch.data.loadIconIndex
import com.immortal521.colorosiconspatch.data.syncIconResources
import com.immortal521.colorosiconspatch.data.loadInstalledApps
import com.immortal521.colorosiconspatch.data.PackageChangeReceiver
import com.immortal521.colorosiconspatch.data.sendLauncherRefresh
import com.immortal521.colorosiconspatch.ui.navigation.MainNavigationBar
import com.immortal521.colorosiconspatch.ui.onboarding.InitializationFlow
import com.immortal521.colorosiconspatch.ui.screen.AppsScreen
import com.immortal521.colorosiconspatch.ui.screen.HomeScreen
import com.immortal521.colorosiconspatch.ui.screen.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.IntentFilter

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
    var iconIndex by remember { mutableStateOf<IconIndexLoadResult?>(null) }
    var syncPlan by remember { mutableStateOf<IconSyncPlan?>(null) }
    var syncProgress by remember { mutableStateOf<IconSyncProgress?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }

    suspend fun refreshSyncPlan() {
        val index = withContext(Dispatchers.IO) { loadIconIndex(context) }
        iconIndexError = index.error
        iconIndex = index
        val apps = withContext(Dispatchers.IO) {
            loadInstalledApps(context, index.adaptedPackages)
        }
        installedApps = apps
        syncPlan = withContext(Dispatchers.IO) {
            buildIconSyncPlan(apps.mapTo(mutableSetOf()) { it.packageName }, index)
        }
    }

    LaunchedEffect(Unit) {
        refreshSyncPlan()
    }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: android.content.Intent) {
                if (intent.action == PackageChangeReceiver.ACTION_PACKAGE_SET_CHANGED) {
                    pagerScope.launch { refreshSyncPlan() }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(PackageChangeReceiver.ACTION_PACKAGE_SET_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
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
                0 -> HomeScreen(
                    contentPadding = mainContentPadding,
                    plan = syncPlan,
                    progress = syncProgress,
                    syncing = syncing,
                    error = syncError,
                    onRefreshLauncher = { sendLauncherRefresh(context) },
                    onSync = {
                        val apps = installedApps ?: return@HomeScreen
                        val index = iconIndex ?: return@HomeScreen
                        val planToSync = syncPlan ?: return@HomeScreen
                        pagerScope.launch {
                            syncing = true
                            syncError = null
                            try {
                                withContext(Dispatchers.IO) {
                                    syncIconResources(context, planToSync) {
                                        syncProgress = it
                                    }
                                }
                                syncPlan = withContext(Dispatchers.IO) {
                                    buildIconSyncPlan(apps.mapTo(mutableSetOf()) { it.packageName }, index)
                                }
                            } catch (error: Exception) {
                                syncError = error.message ?: "资源更新失败"
                            } finally {
                                syncing = false
                            }
                        }
                    }
                )
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
