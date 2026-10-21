package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.highcapable.yukihookapi.YukiHook
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private const val APP_PACKAGE = "com.immortal521.colorosiconspatch"
private const val REFRESH_ACTION = "$APP_PACKAGE.action.REFRESH_LAUNCHER_ICONS"
private val LAUNCHER_PACKAGES = listOf("com.android.launcher", "com.android.launcher3")

val xposedServiceAvailability: Flow<Boolean> = callbackFlow {
    val subscription = YukiHook.Status.observeFrameworkService { trySend(it) }
    awaitClose { subscription?.unregister() }
}

fun sendLauncherRefresh(context: Context) {
    LAUNCHER_PACKAGES.forEach { packageName ->
        context.sendBroadcast(Intent(REFRESH_ACTION).setPackage(packageName))
    }
    LogStore.info(context, "Launcher refresh broadcast sent")
}

suspend fun checkXposedActivation(context: Context): CheckStatus {
    val active = YukiHook.Status.isModuleActive || withTimeoutOrNull(1500L.milliseconds) {
        xposedServiceAvailability.first { it }
    } == true
    val status = if (active) CheckStatus.PASSED else CheckStatus.FAILED
    context.getSharedPreferences("environment_check", Context.MODE_PRIVATE)
        .edit {
            putString("xposed_status", status.name)
        }
    return status
}
