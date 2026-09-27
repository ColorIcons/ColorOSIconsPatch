package com.immortal521.colorosiconspatch

import android.app.Application
import com.immortal521.colorosiconspatch.data.XposedServiceState
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class ColorIconsPatchApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        XposedServiceState.bind(service)
    }

    override fun onServiceDied(service: XposedService) {
        XposedServiceState.unbind(service)
    }
}
