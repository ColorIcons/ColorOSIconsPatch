package com.immortal521.colorosiconspatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.immortal521.colorosiconspatch.data.AppSettingsState
import com.immortal521.colorosiconspatch.ui.App
import com.immortal521.colorosiconspatch.ui.theme.ColorOSIconsPatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppSettingsState.load(this)
        setContent {
            ColorOSIconsPatchTheme {
                App()
            }
        }
    }
}
