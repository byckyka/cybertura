package com.example.cybertura.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Desplaza la pantalla hasta dejar [content] a la vista cada vez que cambia [key] con [active]
 * en true.
 *
 * Los retos son más altos que la pantalla: sin esto, lo que el niño debe tocar a continuación
 * (las respuestas del chat, la explicación de un error) puede aparecer por debajo del borde y
 * pasar desapercibido.
 */
@Composable
fun RevealOnChange(
    key: Any?,
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(key, active) {
        if (active) {
            // Un respiro para que el contenido nuevo termine de medirse antes de desplazar
            delay(REVEAL_DELAY_MS)
            requester.bringIntoView()
        }
    }
    Box(modifier = modifier.bringIntoViewRequester(requester)) { content() }
}

private const val REVEAL_DELAY_MS = 150L
