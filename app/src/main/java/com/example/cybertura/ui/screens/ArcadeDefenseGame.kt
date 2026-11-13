package com.example.cybertura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.ArcadeItem
import com.example.cybertura.game.engine.ArcadeEvent
import com.example.cybertura.game.engine.ArcadeOutcome
import com.example.cybertura.game.engine.ArcadeSpawn
import com.example.cybertura.game.engine.ArcadeWaveSim
import com.example.cybertura.ui.components.RevealOnChange
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.ArcadeGameState
import com.example.cybertura.ui.viewmodel.ArcadePhase
import kotlinx.coroutines.delay

private val ThreatColor = NeonPink
private val SafeColor = NeonGreen
private val ForestTop = Color(0xFF0F2A1A)
private val ForestBottom = Color(0xFF1F5A30)

private const val ITEM_WIDTH_FRACTION = 0.3f
private val ITEM_HEIGHT = 74.dp
private const val MESSAGE_MS = 2400L
/** Pausa al terminar la oleada, para que se alcance a ver el último resultado. */
private const val WAVE_END_PAUSE_MS = 700L

/**
 * Arcade "Defiende el bosque": caen descargas y archivos; el niño toca las amenazas
 * para destruirlas y deja pasar lo seguro. Oleadas cada vez más rápidas, con escudos.
 *
 * Las reglas están en [ArcadeWaveSim]; esta pantalla solo dibuja, da sonido y reporta.
 */
@Composable
fun ArcadeDefenseGame(
    game: ArcadeGameState,
    onStartWave: () -> Unit,
    onWaveEnded: () -> Unit,
    /** Cada objeto resuelto: [tapped] = el niño lo tocó; si no, llegó al bosque. */
    onItemResolved: (item: ArcadeItem, tapped: Boolean) -> Unit = { _, _ -> }
) {
    // Sonido de oleada superada (la última la celebra el modal de victoria)
    LaunchedEffect(game.phase, game.waveIndex) {
        if (game.phase == ArcadePhase.WAVE_CLEAR && !game.finished) SoundManager.playCorrect()
    }

    // Al empezar una oleada el campo entero debe verse: los objetos caen desde arriba
    RevealOnChange(key = game.attempt, active = game.phase == ArcadePhase.PLAYING) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "OLEADA ${game.waveIndex + 1} DE ${game.config.waves.size}",
                color = NeonGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(ForestTop, ForestBottom)))
                    .border(1.5.dp, SafeColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            ) {
                val sim = game.sim
                if (game.phase == ArcadePhase.PLAYING && sim != null) {
                    key(sim) {
                        WaveRunner(sim = sim, onEnded = onWaveEnded, onItemResolved = onItemResolved)
                    }
                } else {
                    // Bosque de fondo mientras se muestra el panel
                    ForestLine(Modifier.align(Alignment.BottomCenter))
                    ArcadePanel(game, onStartWave, Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

/** Fila de árboles al fondo: es lo que hay que proteger. */
@Composable
private fun ForestLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(7) { i -> Text(text = if (i % 2 == 0) "🌲" else "🌳", fontSize = 24.sp) }
    }
}

@Composable
private fun ShieldsRow(shields: Int, maxShields: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(maxShields) { i ->
            Text(text = "🛡️", fontSize = 18.sp, modifier = Modifier.alpha(if (i < shields) 1f else 0.2f))
        }
    }
}

/** Explicación inicial, consejo entre oleadas o aviso de oleada perdida. */
@Composable
private fun ArcadePanel(game: ArcadeGameState, onStartWave: () -> Unit, modifier: Modifier = Modifier) {
    val (title, body, button) = when (game.phase) {
        ArcadePhase.INTRO -> Triple(
            "🌲 ¡Defiende el bosque!",
            "Van a caer descargas y archivos.\n\n👆 Toca los VIRUS y trampas para destruirlos antes de que lleguen a los árboles.\n\n✅ Deja pasar lo SEGURO: si lo tocas, pierdes un escudo.\n\nTienes ${game.shields} escudos por oleada.",
            "¡Empezar!"
        )
        ArcadePhase.WAVE_CLEAR -> Triple(
            if (game.finished) "🏆 ¡Bosque salvado!" else "✨ ¡Oleada superada!",
            "💡 ${game.lastTip ?: "¡Muy bien!"}",
            if (game.finished) null else "Siguiente oleada ➜"
        )
        ArcadePhase.WAVE_FAILED -> Triple(
            "💥 ¡Se colaron los virus!",
            "Te quedaste sin escudos, pero no pasa nada: vuelve a intentarlo. Recuerda: las cosas gratis de páginas raras y los archivos de desconocidos son trampas.",
            "⏪ Repetir oleada"
        )
        ArcadePhase.PLAYING -> return
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DungeonCard.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGold.copy(alpha = 0.6f)),
        modifier = modifier.padding(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = NeonGold, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            if (button != null) {
                Button(
                    onClick = onStartWave,
                    colors = ButtonDefaults.buttonColors(containerColor = SafeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(button, color = DungeonBlack, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** Dibuja una oleada y la hace avanzar cuadro a cuadro. */
@Composable
private fun WaveRunner(
    sim: ArcadeWaveSim,
    onEnded: () -> Unit,
    onItemResolved: (item: ArcadeItem, tapped: Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnItemResolved by rememberUpdatedState(onItemResolved)
    val spawns = sim.spawns
    val maxShields = sim.maxShields

    // La simulación no es estado de Compose: estas copias se actualizan tras cada paso o toque
    // para que la pantalla se redibuje.
    var elapsed by remember { mutableLongStateOf(sim.elapsedMs) }
    var shields by remember { mutableIntStateOf(sim.shields) }
    var resolved by remember { mutableIntStateOf(sim.resolvedCount) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var messageAt by remember { mutableLongStateOf(0L) }
    var areaWidth by remember { mutableIntStateOf(1) }
    var areaHeight by remember { mutableIntStateOf(1) }

    fun sync() {
        elapsed = sim.elapsedMs
        shields = sim.shields
        resolved = sim.resolvedCount
    }

    fun showMessage(text: String, good: Boolean) {
        message = text to good
        messageAt = sim.elapsedMs
    }

    /** Sonido, vibración y mensaje de lo que pasó con un objeto, y aviso para el registro. */
    fun report(event: ArcadeEvent) {
        val item = event.spawn.item
        currentOnItemResolved(item, event.outcome.tapped)
        if (event.outcome.losesShield) {
            SoundManager.playFail()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        when (event.outcome) {
            ArcadeOutcome.DESTROYED -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showMessage("💥 ¡Destruiste: ${item.text}!", good = true)
            }
            ArcadeOutcome.WRONG_TAP -> showMessage("🛡️ -1 · ¡Eso era seguro! ${item.feedback}", good = false)
            ArcadeOutcome.HIT -> showMessage("🛡️ -1 · Se coló: ${item.text}. ${item.feedback}", good = false)
            ArcadeOutcome.PASSED -> showMessage("✅ Dejaste pasar: ${item.text}", good = true)
        }
    }

    fun tap(spawn: ArcadeSpawn) {
        val event = sim.tap(spawn.uid) ?: return
        report(event)
        sync()
    }

    // Bucle de juego: avanza el tiempo con pasos acotados (si la app se pausa, no salta)
    LaunchedEffect(sim) {
        var last = withFrameMillis { it }
        while (!sim.isOver) {
            val now = withFrameMillis { it }
            val events = sim.advance((now - last).coerceIn(0L, 50L))
            last = now
            events.forEach { report(it) }
            sync()
        }
        delay(WAVE_END_PAUSE_MS)
        currentOnEnded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                areaWidth = it.width
                areaHeight = it.height
            }
    ) {
        ForestLine(Modifier.align(Alignment.BottomCenter))

        // Objetos cayendo
        val itemWidthPx = areaWidth * ITEM_WIDTH_FRACTION
        val itemHeightPx = with(density) { ITEM_HEIGHT.toPx() }
        val fallDistance = (areaHeight - itemHeightPx - with(density) { 34.dp.toPx() }).coerceAtLeast(1f)
        // Leer `elapsed` y `resolved` hace que la lista se recalcule en cada paso de la simulación
        val falling = remember(elapsed, resolved) { spawns.filter { sim.isFalling(it) } }
        for (spawn in falling) {
            val progress = sim.progress(spawn)
            val laneX = (areaWidth - itemWidthPx) * (spawn.lane.toFloat() / (ArcadeWaveSim.LANES - 1))
            key(spawn.uid) {
                FallingCard(
                    item = spawn.item,
                    modifier = Modifier
                        .offset { IntOffset(laneX.toInt(), (progress * fallDistance).toInt()) }
                        .width(with(density) { itemWidthPx.toDp() })
                        .height(ITEM_HEIGHT)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { tap(spawn) }
                )
            }
        }

        // Escudos y mensaje
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldsRow(shields.coerceAtLeast(0), maxShields)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$resolved/${spawns.size}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            val msg = message
            if (msg != null && elapsed - messageAt < MESSAGE_MS) {
                val color = if (msg.second) SafeColor else ThreatColor
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DungeonBlack.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = msg.first,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/** Tarjeta que cae. Todas se ven igual: el niño debe leer para decidir. */
@Composable
private fun FallingCard(item: ArcadeItem, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DungeonCard.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = item.emoji, fontSize = 22.sp)
            Text(
                text = item.text,
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
