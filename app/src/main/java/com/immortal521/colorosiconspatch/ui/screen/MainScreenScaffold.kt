package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreenScaffold(
    modifier: Modifier = Modifier,
    title: String,
    contentPadding: PaddingValues = PaddingValues(),
    scrollable: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    topBarContent: @Composable (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val pageInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    )
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = pageInsets,
        topBar = {
            if (topBarContent == null) {
                LargeFlexibleTopAppBar(
                    title = { Text(title) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = topBarColors,
                    windowInsets = pageInsets,
                    scrollBehavior = scrollBehavior
                )
            } else {
                Column {
                    LargeFlexibleTopAppBar(
                        title = { Text(title) },
                        navigationIcon = navigationIcon,
                        actions = actions,
                        colors = topBarColors,
                        windowInsets = pageInsets,
                        scrollBehavior = scrollBehavior
                    )
                    topBarContent()
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(contentPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection)

        if (scrollable) {
            Column(
                modifier = contentModifier.verticalScroll(rememberScrollState())
            ) {
                content(Modifier)
            }
        } else {
            Box(modifier = contentModifier) {
                content(Modifier.fillMaxSize())
            }
        }
    }
}
