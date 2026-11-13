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

        val app = application as CyberturaApplication

        setContent {
            CyberturaTheme {
                CyberturaNavHost(repository = app.repository, researchLogger = app.researchLogger)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SoundManager.onAppForeground()
        (application as CyberturaApplication).researchLogger.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        SoundManager.onAppBackground()
        // El piloto mide el tiempo de juego: lo que pase con la app oculta no cuenta
        (application as CyberturaApplication).researchLogger.onAppBackground()
    }
}