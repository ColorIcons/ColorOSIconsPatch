package com.immortal521.colorosiconspatch

import android.app.Application
import com.immortal521.colorosiconspatch.data.UpdateDownloadCleanup
import com.immortal521.colorosiconspatch.data.XposedServiceState
import com.immortal521.colorosiconspatch.data.loadAppSettings
import com.immortal521.colorosiconspatch.data.setPredictiveBackEnabled
import com.immortal521.colorosiconspatch.data.updateInstalledModuleMetadata
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ColorIconsPatchApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        setPredictiveBackEnabled(this, loadAppSettings(this).predictiveBack)
        UpdateDownloadCleanup.clear(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            updateInstalledModuleMetadata(this@ColorIconsPatchApplication)
        }
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        XposedServiceState.bind(service)
    }

    override fun onServiceDied(service: XposedService) {
        XposedServiceState.unbind(service)
    }
}
