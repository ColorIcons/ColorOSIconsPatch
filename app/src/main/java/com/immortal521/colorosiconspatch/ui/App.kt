package com.immortal521.colorosiconspatch.ui

import android.content.Context
import android.content.IntentFilter
import android.os.Parcelable
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
import androidx.compose.runtime.collectAsState
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
import com.immortal521.colorosiconspatch.data.AppSettingsState
import com.immortal521.colorosiconspatch.data.IconIndexLoadResult
import com.immortal521.colorosiconspatch.data.IconSyncPlan
import com.immortal521.colorosiconspatch.data.IconSyncProgress
import com.immortal521.colorosiconspatch.data.InstalledApp
import com.immortal521.colorosiconspatch.ui.screen.AppDetailScreen
import com.immortal521.colorosiconspatch.data.PackageChangeReceiver
import com.immortal521.colorosiconspatch.data.buildIconSyncPlan
import com.immortal521.colorosiconspatch.data.loadIconIndex
import com.immortal521.colorosiconspatch.data.loadInstalledApps
import com.immortal521.colorosiconspatch.data.sendLauncherRefresh
import com.immortal521.colorosiconspatch.data.syncIconResources
import com.immortal521.colorosiconspatch.ui.navigation.MainNavigationBar
import com.immortal521.colorosiconspatch.ui.onboarding.InitializationFlow
import com.immortal521.colorosiconspatch.ui.screen.AppsScreen
import com.immortal521.colorosiconspatch.ui.screen.DownloadSettingsScreen
import com.immortal521.colorosiconspatch.ui.screen.HomeScreen
import com.immortal521.colorosiconspatch.ui.screen.SettingsPage
import com.immortal521.colorosiconspatch.ui.screen.SettingsScreen
import com.immortal521.colorosiconspatch.ui.screen.ThemeSettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

@Composable
fun App() {
    InitializationFlow {
        MainContent()
    }
}

@Serializable
@Parcelize
private sealed interface AppRoute : NavKey, Parcelable {
    @Serializable
    @Parcelize
    data object Main : AppRoute

    @Serializable
    @Parcelize
    data object ThemeSettings : AppRoute

    @Serializable
    @Parcelize
    data object DownloadSettings : AppRoute

    @Serializable
    @Parcelize
    data class AppDetail(val packageName: String) : AppRoute
}

@Composable
private fun MainContent() {
    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Main)

    NavDisplay(
        backStack = backStack,
        effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
    ) {
        entry<AppRoute.Main>(swipeDismiss = NavSwipeDirection.LeftToRight) {
            MainPagerScreen(
                onOpenSettingsPage = { page ->
                when (page) {
                    SettingsPage.THEME -> backStack.add(AppRoute.ThemeSettings)
                    SettingsPage.DOWNLOAD -> backStack.add(AppRoute.DownloadSettings)
                    SettingsPage.ROOT -> Unit
                }
            },
                onOpenApp = { app -> backStack.add(AppRoute.AppDetail(app.packageName)) }
            )
        }
        entry<AppRoute.ThemeSettings>(swipeDismiss = NavSwipeDirection.LeftToRight) {
            ThemeSettingsScreen(onBack = { backStack.removeLastOrNull() })
        }
        entry<AppRoute.DownloadSettings>(swipeDismiss = NavSwipeDirection.LeftToRight) {
            DownloadSettingsScreen(onBack = { backStack.removeLastOrNull() })
        }
        entry<AppRoute.AppDetail>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            AppDetailScreen(
                packageName = route.packageName,
                onBack = { backStack.removeLastOrNull() }
            )
        }
    }
}

@Composable
private fun MainPagerScreen(
    onOpenSettingsPage: (SettingsPage) -> Unit,
    onOpenApp: (InstalledApp) -> Unit
) {
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
    val appSettings by AppSettingsState.settings.collectAsState()

    suspend fun refreshSyncPlan() {
        val index = withContext(Dispatchers.IO) {
            loadIconIndex(context, appSettings.indexUrl, appSettings.variants)
        }
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

    LaunchedEffect(appSettings.channel, appSettings.variants) {
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
        context.registerReceiver(
            receiver,
            IntentFilter(PackageChangeReceiver.ACTION_PACKAGE_SET_CHANGED),
            Context.RECEIVER_NOT_EXPORTED
        )
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
                onTabSelected = { tab ->
                    selectedTab = tab
                    pagerScope.launch { pagerState.animateScrollToPage(tab) }
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
                    contentPadding = mainContentPadding,
                    onAppClick = onOpenApp
                )

                2 -> SettingsScreen(
                    contentPadding = mainContentPadding,
                    onOpen = onOpenSettingsPage
                )
            }
        }
    }
}
