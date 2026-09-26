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
import com.immortal521.colorosiconspatch.data.ModuleOperationStatus
import com.immortal521.colorosiconspatch.data.checkModule
import com.immortal521.colorosiconspatch.data.checkRoot
import com.immortal521.colorosiconspatch.data.prepareAndInstallModule
import com.immortal521.colorosiconspatch.data.loadEnvironmentCheck
import com.immortal521.colorosiconspatch.ui.screen.WelcomeScreen
import androidx.core.content.edit

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
                preferences.edit { putBoolean(COMPLETED, true) }
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
    var moduleInstallStatus by remember { mutableStateOf<ModuleOperationStatus?>(null) }
    var moduleInstallMessage by remember { mutableStateOf<String?>(null) }
    var installRequested by remember { mutableStateOf(false) }

    fun requestRoot() {
        environmentCheck = environmentCheck.copy(root = CheckStatus.CHECKING)
    }

    fun installModule() {
        installRequested = true
        moduleInstallStatus = null
        moduleInstallMessage = null
        environmentCheck = environmentCheck.copy(module = CheckStatus.CHECKING)
    }

    LaunchedEffect(environmentCheck.root) {
        if (environmentCheck.root == CheckStatus.CHECKING) {
            environmentCheck = environmentCheck.copy(root = checkRoot(context))
        }
    }

    LaunchedEffect(environmentCheck.root, environmentCheck.module) {
        if (environmentCheck.root == CheckStatus.PASSED && environmentCheck.module == CheckStatus.UNKNOWN) {
            environmentCheck = environmentCheck.copy(module = checkModule(context))
        } else if (environmentCheck.module == CheckStatus.CHECKING) {
            if (installRequested) {
                val result = prepareAndInstallModule(context)
                moduleInstallStatus = result.status
                moduleInstallMessage = result.message
                installRequested = false
            }
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
            rootImplementation = environmentCheck.implementation,
            onRequestRoot = ::requestRoot,
            onInstallModule = ::installModule,
            moduleInstallStatus = moduleInstallStatus,
            moduleInstallMessage = moduleInstallMessage,
            onContinue = onComplete
        )
    }
}
