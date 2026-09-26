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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.immortal521.colorosiconspatch.ui.navigation.MainNavigationBar
import com.immortal521.colorosiconspatch.ui.screen.AppsScreen
import com.immortal521.colorosiconspatch.ui.screen.HomeScreen
import com.immortal521.colorosiconspatch.ui.screen.SettingsScreen
import com.immortal521.colorosiconspatch.ui.screen.WelcomeScreen

@Composable
fun App() {
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

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
                    onTabSelected = { selectedTab = it }
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

            when (selectedTab) {
                0 -> HomeScreen(contentPadding = mainContentPadding)
                1 -> AppsScreen(contentPadding = mainContentPadding)
                2 -> SettingsScreen(contentPadding = mainContentPadding)
            }
        }
    }
}
