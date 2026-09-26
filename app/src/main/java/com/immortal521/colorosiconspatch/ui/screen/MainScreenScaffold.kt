package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    title: String,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable (Modifier) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    val pageInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    )
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.background
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = pageInsets,
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                expandedHeight = 120.dp,
                colors = topBarColors,
                windowInsets = pageInsets,
                scrollBehavior = scrollBehavior
            )
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
