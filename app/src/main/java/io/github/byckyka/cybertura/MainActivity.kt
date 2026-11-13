package io.github.byckyka.cybertura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import io.github.byckyka.cybertura.audio.SoundManager
import io.github.byckyka.cybertura.ui.components.LocalNarrator
import io.github.byckyka.cybertura.ui.navigation.CyberturaNavHost
import io.github.byckyka.cybertura.ui.theme.CyberturaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CyberturaApplication

        setContent {
            CyberturaTheme {
                CompositionLocalProvider(LocalNarrator provides app.narrator) {
                    CyberturaNavHost(repository = app.repository, researchLogger = app.researchLogger)
                }
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
        (application as CyberturaApplication).narrator.stop()
        // El piloto mide el tiempo de juego: lo que pase con la app oculta no cuenta
        (application as CyberturaApplication).researchLogger.onAppBackground()
    }
}