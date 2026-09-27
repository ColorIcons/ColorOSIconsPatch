package com.immortal521.colorosiconspatch

import android.app.Application
import com.immortal521.colorosiconspatch.data.UpdateDownloadCleanup
import com.immortal521.colorosiconspatch.data.XposedServiceState
import com.immortal521.colorosiconspatch.data.loadAppSettings
import com.immortal521.colorosiconspatch.data.setPredictiveBackEnabled
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class ColorIconsPatchApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        setPredictiveBackEnabled(this, loadAppSettings(this).predictiveBack)
        UpdateDownloadCleanup.clear(this)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        XposedServiceState.bind(service)
    }

    override fun onServiceDied(service: XposedService) {
        XposedServiceState.unbind(service)
    }
}
