package com.immortal521.colorosiconspatch.xposed

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.ref.WeakReference

class MainModule : XposedModule() {
    companion object {
        private const val TAG = "ColorOSIconsPatch"
        private const val ACTION_REFRESH = "com.immortal.coloros.iconpatch.REFRESH_ICONS"
        private const val LAUNCHER_PACKAGE = "com.android.launcher"
        private const val LAUNCHER3_PACKAGE = "com.android.launcher3"

        private var launcherRef = WeakReference<Context>(null)
        private var receiver: BroadcastReceiver? = null
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        log(Log.INFO, TAG, "Module loaded in ${param.processName}")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val packageName = param.packageName
        if (packageName != LAUNCHER_PACKAGE && packageName != LAUNCHER3_PACKAGE) return

        val launcherClassName = when (packageName) {
            LAUNCHER3_PACKAGE -> "com.android.launcher3.Launcher"
            else -> "com.android.launcher.Launcher"
        }

        try {
            val launcherClass = param.defaultClassLoader.loadClass(launcherClassName)
            val onCreate = launcherClass.getDeclaredMethod("onCreate", Bundle::class.java)
            hook(onCreate).intercept { chain ->
                val result = chain.proceed()
                val context = chain.thisObject as? Context
                if (context != null) {
                    launcherRef = WeakReference(context)
                    registerReceiver(context)
                    log(Log.INFO, TAG, "Launcher captured: $packageName")
                }
                result
            }
            log(Log.INFO, TAG, "Hook installed: $launcherClassName.onCreate")
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Failed to hook $launcherClassName", error)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver(context: Context) {
        if (receiver != null) return
        val refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_REFRESH) refreshLauncher()
            }
        }
        try {
            context.registerReceiver(
                refreshReceiver,
                IntentFilter(ACTION_REFRESH),
                Context.RECEIVER_EXPORTED
            )
            receiver = refreshReceiver
            log(Log.INFO, TAG, "Refresh receiver registered")
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Failed to register refresh receiver", error)
        }
    }

    private fun refreshLauncher() {
        val context = launcherRef.get() ?: return
        try {
            val appStateClass = context.classLoader.loadClass("com.android.launcher3.LauncherAppState")
            val getInstance = appStateClass.getMethod("getInstance", Context::class.java)
            val appState = getInstance.invoke(null, context)
            val refresh = appStateClass.getDeclaredMethod("refreshAndReloadLauncher")
            refresh.isAccessible = true
            refresh.invoke(appState)
            log(Log.INFO, TAG, "Launcher icons refreshed")
        } catch (error: NoSuchMethodException) {
            refreshWithModelFallback(context)
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Failed to refresh launcher icons", error)
        }
    }

    private fun refreshWithModelFallback(context: Context) {
        try {
            val appStateClass = context.classLoader.loadClass("com.android.launcher3.LauncherAppState")
            val appState = appStateClass.getMethod("getInstance", Context::class.java).invoke(null, context)
            val model = appStateClass.getMethod("getModel").invoke(appState)
            model.javaClass.getMethod("forceReload").invoke(model)
            log(Log.INFO, TAG, "Launcher model reloaded")
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Launcher model reload failed", error)
        }
    }
}
