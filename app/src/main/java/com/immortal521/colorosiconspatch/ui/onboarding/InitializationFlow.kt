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
import com.immortal521.colorosiconspatch.data.checkXposedActivation
import com.immortal521.colorosiconspatch.data.checkRoot
import com.immortal521.colorosiconspatch.data.prepareAndInstallModule
import com.immortal521.colorosiconspatch.data.loadEnvironmentCheck
import com.immortal521.colorosiconspatch.data.rebootDevice
import com.immortal521.colorosiconspatch.ui.screen.WelcomeScreen
import androidx.core.content.edit
import android.provider.Settings

private const val PREFERENCES = "onboarding"
private const val COMPLETED = "completed"
private const val REBOOT_BOOT_COUNT = "reboot_boot_count"

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
    val preferences = remember {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    }
    var environmentCheck by remember {
        mutableStateOf(loadEnvironmentCheck(context))
    }
    var moduleInstallStatus by remember { mutableStateOf<ModuleOperationStatus?>(null) }
    var moduleInstallMessage by remember { mutableStateOf<String?>(null) }
    var installRequested by remember { mutableStateOf(false) }
    var rebootRequiredBootCount by remember {
        mutableStateOf(preferences.getLong(REBOOT_BOOT_COUNT, -1L))
    }
    val currentBootCount = remember {
        Settings.Global.getLong(context.contentResolver, Settings.Global.BOOT_COUNT, -1L)
    }

    fun requestRoot() {
        environmentCheck = environmentCheck.copy(root = CheckStatus.CHECKING)
    }

    fun installModule() {
        installRequested = true
        moduleInstallStatus = null
        moduleInstallMessage = null
        environmentCheck = environmentCheck.copy(module = CheckStatus.CHECKING)
    }

    fun checkXposed() {
        environmentCheck = environmentCheck.copy(xposed = CheckStatus.UNKNOWN)
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

    LaunchedEffect(environmentCheck.root, environmentCheck.module) {
        if (environmentCheck.root == CheckStatus.PASSED &&
            environmentCheck.module == CheckStatus.PASSED
        ) {
            environmentCheck = environmentCheck.copy(xposed = CheckStatus.CHECKING)
            environmentCheck = environmentCheck.copy(xposed = checkXposedActivation(context))
            if (rebootRequiredBootCount < 0L) {
                rebootRequiredBootCount = currentBootCount
                preferences.edit { putLong(REBOOT_BOOT_COUNT, currentBootCount) }
            }
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
            xposedStatus = environmentCheck.xposed,
            rootImplementation = environmentCheck.implementation,
            onRequestRoot = ::requestRoot,
            onInstallModule = ::installModule,
            moduleInstallStatus = moduleInstallStatus,
            moduleInstallMessage = moduleInstallMessage,
            onContinue = onComplete,
            onCheckXposed = ::checkXposed,
            onSkipXposed = {
                environmentCheck = environmentCheck.copy(xposed = CheckStatus.PASSED)
                if (rebootRequiredBootCount < 0L) {
                    rebootRequiredBootCount = currentBootCount
                    preferences.edit { putLong(REBOOT_BOOT_COUNT, currentBootCount) }
                }
            },
            rebootReady = rebootRequiredBootCount >= 0L && currentBootCount != rebootRequiredBootCount,
            onReboot = { rebootDevice() }
        )
    }
}
