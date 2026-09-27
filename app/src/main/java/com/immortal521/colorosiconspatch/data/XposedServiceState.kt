package com.immortal521.colorosiconspatch.data

import io.github.libxposed.service.XposedService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object XposedServiceState {
    private val _service = MutableStateFlow<XposedService?>(null)
    val service: StateFlow<XposedService?> = _service.asStateFlow()

    val active: Boolean
        get() = _service.value != null

    fun bind(service: XposedService) {
        _service.value = service
    }

    fun unbind(service: XposedService) {
        if (_service.value === service) _service.value = null
    }
}
