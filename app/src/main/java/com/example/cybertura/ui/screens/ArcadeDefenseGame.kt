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
import com.example.cybertura.data.model.ArcadeWave
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.ArcadeGameState
import com.example.cybertura.ui.viewmodel.ArcadePhase
import kotlinx.coroutines.delay
import kotlin.random.Random

private val ThreatColor = NeonPink
private val SafeColor = NeonGreen
private val ForestTop = Color(0xFF0F2A1A)
private val ForestBottom = Color(0xFF1F5A30)

private const val ITEM_WIDTH_FRACTION = 0.3f
private val ITEM_HEIGHT = 74.dp
private const val THREAT_RATIO = 0.6f
private const val MESSAGE_MS = 2400L

/** Objeto que cae durante una oleada. */
private class Spawn(val uid: Int, val item: ArcadeItem, val lane: Int, val spawnAt: Long)

private enum class Outcome { DESTROYED, PASSED, HIT, WRONG_TAP }

/**
 * Arcade "Defiende el bosque": caen descargas y archivos; el niño toca las amenazas
 * para destruirlas y deja pasar lo seguro. Oleadas cada vez más rápidas, con escudos.
 */
@Composable
fun ArcadeDefenseGame(
    game: ArcadeGameState,
    onStartWave: () -> Unit,
    onWaveEnded: (shieldsLost: Int, failed: Boolean) -> Unit
) {
    // Sonido de oleada superada (la última la celebra el modal de victoria)
    LaunchedEffect(game.phase, game.waveIndex) {
        if (game.phase == ArcadePhase.WAVE_CLEAR && !game.finished) SoundManager.playCorrect()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "OLEADA ${game.waveIndex + 1} DE ${game.config.waves.size}",
            color = NeonGold,
            fontSize = 11.sp,
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
            if (game.phase == ArcadePhase.PLAYING) {
                key(game.waveIndex, game.attempt) {
                    WaveRunner(
                        wave = game.wave,
                        pool = game.config.items,
                        maxShields = game.shields,
                        onEnded = onWaveEnded
                    )
                }
            } else {
                // Bosque de fondo mientras se muestra el panel
                ForestLine(Modifier.align(Alignment.BottomCenter))
                ArcadePanel(game, onStartWave, Modifier.align(Alignment.Center))
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

/** Arma la lista de objetos de la oleada: ~60 % amenazas, sin repetir carril dos veces seguidas. */
private fun buildWave(wave: ArcadeWave, pool: List<ArcadeItem>): List<Spawn> {
    val threats = pool.filter { it.isThreat }.shuffled()
    val safes = pool.filter { !it.isThreat }.shuffled()
    val spawnMs = wave.spawnMs ?: 1200L
    var lastLane = -1
    return List(wave.count) { i ->
        val useThreat = safes.isEmpty() || (threats.isNotEmpty() && Random.nextFloat() < THREAT_RATIO)
        val item = if (useThreat) threats[i % threats.size] else safes[i % safes.size]
        val lane = (0..2).filter { it != lastLane }.random().also { lastLane = it }
        Spawn(uid = i, item = item, lane = lane, spawnAt = 600L + i * spawnMs)
    }
}

/** Simulación de una oleada, cuadro a cuadro. */
@Composable
private fun WaveRunner(
    wave: ArcadeWave,
    pool: List<ArcadeItem>,
    maxShields: Int,
    onEnded: (shieldsLost: Int, failed: Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val currentOnEnded by rememberUpdatedState(onEnded)
    val spawns = remember { buildWave(wave, pool) }
    val fallMs = wave.fallMs ?: 6000L
    val outcomes = remember { mutableStateMapOf<Int, Outcome>() }
    var elapsed by remember { mutableLongStateOf(0L) }
    var shields by remember { mutableIntStateOf(maxShields) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var messageAt by remember { mutableLongStateOf(0L) }
    var areaWidth by remember { mutableIntStateOf(1) }
    var areaHeight by remember { mutableIntStateOf(1) }
    var ended by remember { mutableStateOf(false) }

    fun showMessage(text: String, good: Boolean) {
        message = text to good
        messageAt = elapsed
    }

    fun loseShield(text: String) {
        shields -= 1
        SoundManager.playFail()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        showMessage(text, good = false)
    }

    fun tap(spawn: Spawn) {
        if (ended || spawn.uid in outcomes) return
        if (spawn.item.isThreat) {
            outcomes[spawn.uid] = Outcome.DESTROYED
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showMessage("💥 ¡Destruiste: ${spawn.item.text}!", good = true)
        } else {
            outcomes[spawn.uid] = Outcome.WRONG_TAP
            loseShield("🛡️ -1 · ¡Eso era seguro! ${spawn.item.feedback}")
        }
    }

    // Bucle de juego: avanza el tiempo con pasos acotados (si la app se pausa, no salta)
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (!ended) {
            val now = withFrameMillis { it }
            elapsed += (now - last).coerceIn(0L, 50L)
            last = now
            for (spawn in spawns) {
                if (spawn.uid in outcomes || elapsed < spawn.spawnAt + fallMs) continue
                if (spawn.item.isThreat) {
                    outcomes[spawn.uid] = Outcome.HIT
                    loseShield("🛡️ -1 · Se coló: ${spawn.item.text}. ${spawn.item.feedback}")
                } else {
                    outcomes[spawn.uid] = Outcome.PASSED
                    showMessage("✅ Dejaste pasar: ${spawn.item.text}", good = true)
                }
            }
            if (shields <= 0) {
                ended = true
                delay(700)
                currentOnEnded(maxShields, true)
            } else if (outcomes.size == spawns.size) {
                ended = true
                delay(700)
                currentOnEnded(maxShields - shields, false)
            }
        }
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
        for (spawn in spawns) {
            if (spawn.uid in outcomes || elapsed < spawn.spawnAt) continue
            val progress = ((elapsed - spawn.spawnAt).toFloat() / fallMs).coerceIn(0f, 1f)
            val laneX = (areaWidth - itemWidthPx) * (spawn.lane / 2f)
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
                    text = "${outcomes.size}/${spawns.size}",
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
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
