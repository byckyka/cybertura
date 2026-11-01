package com.example.cybertura.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.game.engine.NpcRegistry
import com.example.cybertura.ui.components.FloatingParticles
import com.example.cybertura.ui.theme.*
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
// CONTENIDO DE LOS CRÉDITOS — editar aquí para agregar o cambiar nombres
// ═══════════════════════════════════════════════════════════════

/** Una línea de los créditos: un nombre y, opcionalmente, una descripción debajo. */
private data class CreditLine(val main: String, val detail: String? = null)

private data class CreditSection(val title: String, val lines: List<CreditLine>)

private val creditSections: List<CreditSection>
    get() = listOf(
        CreditSection(
            title = "Autor",
            lines = listOf(CreditLine("Santiago Matajira Rueda"))
        ),
        CreditSection(
            title = "Director",
            lines = listOf(CreditLine("Javier Pinzón"))
        ),
        CreditSection(
            title = "Asesores",
            lines = listOf(
                CreditLine("Yamid Gamba"),
                CreditLine("Daniel Cabrera"),
                CreditLine("Leonardo Talero"),
                CreditLine("Leonardo Pardo")
            )
        ),
        CreditSection(
            title = "Trabajo de grado",
            lines = listOf(
                CreditLine(
                    "Prototipo de juego móvil educativo",
                    "Para el aprendizaje de conceptos básicos de ciberseguridad en niños de 11 a 12 años de Bucaramanga"
                ),
                CreditLine("Universidad Autónoma de Bucaramanga", "Facultad de Ingeniería · Ingeniería de Sistemas"),
                CreditLine("Bucaramanga, Santander", "2026")
            )
        ),
        // Los maestros salen del registro de NPC, así que siempre coinciden con el juego
        CreditSection(
            title = "Los Maestros Cyber",
            lines = NpcRegistry.questNpcs.map { CreditLine("${it.emoji} ${it.name}", it.title) }
        ),
        CreditSection(
            title = "Hecho con",
            lines = listOf(
                CreditLine("Kotlin y Jetpack Compose"),
                CreditLine("Motor de juego propio", "Dibujado sobre Canvas, sin Unity")
            )
        ),
        CreditSection(
            title = "Recursos gráficos y sonidos",
            lines = listOf(
                CreditLine("Pixabay", "Sprites, escenarios, música y efectos de sonido · pixabay.com"),
                CreditLine("Licencia de contenido de Pixabay", "Gracias a los artistas que comparten su trabajo")
            )
        )
    )

// ═══════════════════════════════════════════════════════════════

/** Velocidad del desplazamiento automático de los créditos. */
private val SCROLL_SPEED_PER_SECOND = 45.dp

@Composable
fun CreditsScreen(
    agentName: String,
    onFinish: () -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Desplazamiento automático tipo película. Si el jugador arrastra, su gesto
    // tiene prioridad y la animación se detiene.
    LaunchedEffect(scrollState.maxValue) {
        val max = scrollState.maxValue
        if (max <= 0 || scrollState.value >= max) return@LaunchedEffect
        delay(1200)
        val speedPx = with(density) { SCROLL_SPEED_PER_SECOND.toPx() }
        val remaining = max - scrollState.value
        scrollState.animateScrollTo(
            value = max,
            animationSpec = tween(
                durationMillis = (remaining / speedPx * 1000).toInt(),
                easing = LinearEasing
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DungeonBlack, DungeonDark, DungeonBlack)))
    ) {
        val screenHeight = maxHeight

        FloatingParticles(
            modifier = Modifier.fillMaxSize(),
            particleColor = NeonGold.copy(alpha = 0.08f),
            count = 14
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Los créditos entran desde la parte baja de la pantalla
            Spacer(modifier = Modifier.height(screenHeight * 0.55f))

            Text(
                text = "CyberTura",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Text(
                text = "Una aventura de ciberseguridad",
                fontSize = 15.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            creditSections.forEach { section ->
                Spacer(modifier = Modifier.height(56.dp))
                CreditSectionBlock(section)
            }

            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = "¡Gracias por jugar,\nAgente ${agentName}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = NeonGold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Recuerda: en internet, piensa antes de hacer clic y trata a los demás con respeto.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(
                    "Volver al Mundo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = DungeonBlack
                )
            }

            // Margen final para que el botón quede centrado al terminar el desplazamiento
            Spacer(modifier = Modifier.height(screenHeight * 0.35f))
        }

        // Saltar los créditos en cualquier momento
        TextButton(
            onClick = onFinish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .background(DungeonCard.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .border(1.dp, DungeonBorder, RoundedCornerShape(12.dp))
        ) {
            Text("Saltar ⏭", color = TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CreditSectionBlock(section: CreditSection) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = section.title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = NeonGold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        section.lines.forEach { line ->
            Text(
                text = line.main,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            line.detail?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
