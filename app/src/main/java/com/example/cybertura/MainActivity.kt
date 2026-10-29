package com.example.cybertura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.ui.navigation.CyberturaNavHost
import com.example.cybertura.ui.theme.CyberturaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as CyberturaApplication).repository

        setContent {
            CyberturaTheme {
                CyberturaNavHost(repository = repository)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SoundManager.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        SoundManager.onAppBackground()
    }
}