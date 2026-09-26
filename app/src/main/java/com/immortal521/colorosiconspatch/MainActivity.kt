package com.immortal521.colorosiconspatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.immortal521.colorosiconspatch.ui.theme.ColorOSIconsPatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorOSIconsPatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp)
    ) {
        Text("系统信息")
        Text("Android 版本：暂未读取")
        Text("设备型号：暂未读取")
        Text("KernelSU：暂未检查")
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ColorOSIconsPatchTheme {
        HomeScreen()
    }
}