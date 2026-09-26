package com.immortal521.colorosiconspatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.immortal521.colorosiconspatch.ui.navigation.MainNavigationBar
import com.immortal521.colorosiconspatch.ui.screen.AppsScreen
import com.immortal521.colorosiconspatch.ui.screen.HomeScreen
import com.immortal521.colorosiconspatch.ui.screen.SettingsScreen
import com.immortal521.colorosiconspatch.ui.screen.WelcomeScreen

@Composable
fun App() {
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedTab) { 3 }
    val pagerScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
            .collect { selectedTab = it }
    }

    BackHandler(enabled = !showWelcome && selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showWelcome) {
                MainNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        selectedTab = it
                        pagerScope.launch { pagerState.animateScrollToPage(it) }
                    }
                )
            }
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = showWelcome,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
        ) {
            WelcomeScreen(
                modifier = Modifier.padding(innerPadding),
                onContinue = { showWelcome = false }
            )
        }

        AnimatedVisibility(
            visible = !showWelcome,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
                    1 -> AppsScreen(contentPadding = mainContentPadding)
                    2 -> SettingsScreen(contentPadding = mainContentPadding)
                }
            }
        }
    }
}
