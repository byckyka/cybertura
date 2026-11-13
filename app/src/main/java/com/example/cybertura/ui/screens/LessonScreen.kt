package com.example.cybertura.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.LessonCard
import com.example.cybertura.ui.components.NarrateButton
import com.example.cybertura.ui.theme.DungeonBlack
import com.example.cybertura.ui.theme.DungeonBorder
import com.example.cybertura.ui.theme.DungeonCard
import com.example.cybertura.ui.theme.DungeonCardHover
import com.example.cybertura.ui.theme.DungeonDark
import com.example.cybertura.ui.theme.NeonCyan
import com.example.cybertura.ui.theme.NeonPink
import com.example.cybertura.ui.theme.TextMuted
import com.example.cybertura.ui.theme.TextPrimary
import com.example.cybertura.ui.theme.TextSecondary

/**
 * Mini lección del módulo: pocas tarjetas, una idea por tarjeta, antes del minijuego.
 *
 * Sin ella el niño llegaba al reto sin saber qué buscar y aprendía solo por ensayo y error.
 * Quien ya superó el reto puede saltarla al repasar.
 */
@Composable
fun LessonScreen(
    module: CyberModule,
    /** El jugador ya completó el reto que sigue: puede saltar la lección. */
    canSkip: Boolean,
    onStartChallenge: () -> Unit,
    onBackClick: () -> Unit
) {
    val cards = module.lesson?.cards.orEmpty()
    val accent = try {
        Color(android.graphics.Color.parseColor(module.accentColor))
    } catch (e: IllegalArgumentException) {
        NeonCyan
    }
    // rememberSaveable: al girar el teléfono la lección sigue en la misma tarjeta
    var index by rememberSaveable { mutableIntStateOf(0) }
    val isLast = index >= cards.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DungeonDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Encabezado ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(DungeonCardHover, CircleShape)
                    .border(1.dp, DungeonBorder, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Salir de la lección", tint = NeonPink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📖 ANTES DE JUGAR",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(text = module.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (canSkip) {
                TextButton(onClick = onStartChallenge) {
                    Text("Saltar", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }

        ProgressDots(count = cards.size, current = index, accent = accent)

        // ── Tarjeta ──
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "lessonCard"
        ) { shown ->
            cards.getOrNull(shown)?.let { LessonCardView(it, accent) }
        }

        // ── Navegación ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (index > 0) {
                TextButton(onClick = { index -= 1 }) {
                    Text("← Anterior", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { if (isLast) onStartChallenge() else index += 1 },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Text(
                    text = if (isLast) "⚔️ ¡A jugar!" else "Siguiente →",
                    color = DungeonBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ProgressDots(count: Int, current: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (i == current) 28.dp else 12.dp)
                    .background(if (i <= current) accent else DungeonBorder, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun LessonCardView(card: LessonCard, accent: Color) {
    Surface(
        color = DungeonCard,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent)))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            // La ilustración es un emoji sobre un círculo dibujado: no usa imágenes externas
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), DungeonBlack)), CircleShape)
                    .border(2.dp, accent.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = card.emoji ?: "💡", fontSize = 64.sp)
            }
            Text(
                text = card.title,
                color = TextPrimary,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = card.text,
                color = TextSecondary,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Center
            )
            NarrateButton(text = "${card.title}. ${card.text}", tint = accent)
            Text(text = "Lee con calma: te servirá en el reto.", color = TextMuted, fontSize = 13.sp)
        }
    }
}
