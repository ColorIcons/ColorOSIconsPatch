package com.immortal521.colorosiconspatch.ui.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.immortal521.colorosiconspatch.data.CheckStatus
import com.immortal521.colorosiconspatch.data.EnvironmentCheckResult
import com.immortal521.colorosiconspatch.data.checkModule
import com.immortal521.colorosiconspatch.data.checkRoot
import com.immortal521.colorosiconspatch.data.loadEnvironmentCheck
import com.immortal521.colorosiconspatch.ui.screen.WelcomeScreen

private const val PREFERENCES = "onboarding"
private const val COMPLETED = "completed"

@Composable
fun InitializationFlow(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    }
    var initialized by remember {
        mutableStateOf(preferences.getBoolean(COMPLETED, false))
    }

    if (initialized) {
        content()
    } else {
        InitializationScreen(
            context = context,
            onComplete = {
                preferences.edit().putBoolean(COMPLETED, true).apply()
                initialized = true
            }
        )
    }
}

@Composable
private fun InitializationScreen(
    context: Context,
    onComplete: () -> Unit
) {
    var environmentCheck by remember {
        mutableStateOf(loadEnvironmentCheck(context))
    }

    fun requestRoot() {
        environmentCheck = environmentCheck.copy(root = CheckStatus.CHECKING)
    }

    fun requestModuleCheck() {
        environmentCheck = environmentCheck.copy(module = CheckStatus.CHECKING)
    }

    LaunchedEffect(environmentCheck.root) {
        if (environmentCheck.root == CheckStatus.CHECKING) {
            environmentCheck = environmentCheck.copy(root = checkRoot(context))
        }
    }

    LaunchedEffect(environmentCheck.module) {
        if (environmentCheck.module == CheckStatus.CHECKING) {
            environmentCheck = environmentCheck.copy(module = checkModule(context))
        }
    }

    AnimatedVisibility(
        visible = true,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
    ) {
        WelcomeScreen(
            modifier = Modifier.fillMaxSize(),
            rootStatus = environmentCheck.root,
            moduleStatus = environmentCheck.module,
            onRequestRoot = ::requestRoot,
            onCheckModule = ::requestModuleCheck,
            onContinue = onComplete
        )
    }
}
