package io.github.byckyka.cybertura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.byckyka.cybertura.audio.Narrator
import io.github.byckyka.cybertura.ui.theme.DungeonCardHover
import io.github.byckyka.cybertura.ui.theme.NeonCyan

/** Narrador de la app; null en las vistas previas y en pruebas, donde no hay voz. */
val LocalNarrator = staticCompositionLocalOf<Narrator?> { null }

/**
 * Botón 🔊 que lee [text] en voz alta; al tocarlo otra vez, se calla.
 * No se dibuja si el teléfono no tiene voz en español.
 */
@Composable
fun NarrateButton(text: String, modifier: Modifier = Modifier, tint: Color = NeonCyan) {
    val narrator = LocalNarrator.current ?: return
    val available by narrator.available.collectAsState()
    if (!available || text.isBlank()) return
    val speaking by narrator.speaking.collectAsState()
    val isThisOne = speaking == text

    // Si el texto sale de la pantalla mientras suena, la voz se detiene
    DisposableEffect(text) {
        onDispose { if (narrator.speaking.value == text) narrator.stop() }
    }

    IconButton(
        onClick = { if (isThisOne) narrator.stop() else narrator.speak(text) },
        modifier = modifier
            .size(40.dp)
            .background(DungeonCardHover, CircleShape)
            .border(1.dp, tint.copy(alpha = 0.6f), CircleShape)
    ) {
        Icon(
            imageVector = if (isThisOne) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isThisOne) "Detener la lectura" else "Leer en voz alta",
            tint = tint
        )
    }
}
