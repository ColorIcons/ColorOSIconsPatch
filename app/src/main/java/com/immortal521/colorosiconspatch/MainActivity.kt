package com.immortal521.colorosiconspatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.immortal521.colorosiconspatch.ui.App
import com.immortal521.colorosiconspatch.ui.theme.ColorOSIconsPatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorOSIconsPatchTheme {
                App()
            }
        }
    }
}
