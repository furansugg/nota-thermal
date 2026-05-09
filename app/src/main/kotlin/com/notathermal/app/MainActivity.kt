package com.notathermal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notathermal.app.ui.NotaApp
import com.notathermal.app.ui.theme.NotaThermalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotaThermalTheme {
                NotaApp()
            }
        }
    }
}
