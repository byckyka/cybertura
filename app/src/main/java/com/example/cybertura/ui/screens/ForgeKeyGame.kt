package com.example.cybertura.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.ForgeBlock
import com.example.cybertura.game.engine.KeyStrength
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.ForgeGameState
import com.example.cybertura.ui.viewmodel.ForgePhase

private val WordColor = NeonCyan
private val NumberColor = NeonGold
private val SymbolColor = NeonPink
private val GoodColor = NeonGreen
private val BadColor = NeonPink

/** Colores del medidor: madera, piedra, hierro, diamante. */
private val MaterialColors = listOf(
    Color(0xFFB07A4A),
    Color(0xFF9AA3AD),
    Color(0xFF7FB3D5),
    Color(0xFF7FF0FF)
)

private fun blockColor(block: ForgeBlock): Color = when {
    block.text.all { it.isDigit() } -> NumberColor
    block.text.none { it.isLetterOrDigit() } -> SymbolColor
    else -> WordColor
}

/**
 * Minijuego "Forja de llaves": rondas para construir contraseñas con bloques
 * (con un medidor de dureza en vivo) y para defenderlas de un ladrón.
 */
@Composable
fun ForgeKeyGame(
    game: ForgeGameState,
    onToggleBlock: (blockId: String) -> Unit,
    onForge: () -> Unit,
    onChooseOption: (optionId: String) -> Unit,
    onContinue: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val hammer = remember { Animatable(0f) }

    // Cada resultado: golpe de martillo (sacudida), sonido y vibración si falló
    LaunchedEffect(game.eventCount) {
        if (game.eventCount == 0) return@LaunchedEffect
        when (game.phase) {
            ForgePhase.SUCCESS -> if (!game.isLastRound) SoundManager.playCorrect()
            ForgePhase.FAIL -> {
                SoundManager.playFail()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> Unit
        }
        for (x in listOf(12f, -10f, 7f, -4f, 0f)) hammer.animateTo(x, tween(45))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundHeader(game)

        val round = game.round
        if (round.isDefend) {
            DefendRound(game, onChooseOption, Modifier.graphicsLayer { translationX = hammer.value })
        } else {
            BuildRound(game, onToggleBlock, onForge, Modifier.graphicsLayer { translationX = hammer.value })
        }

        if (game.phase == ForgePhase.SUCCESS || game.phase == ForgePhase.FAIL) {
            ResultPanel(game, onContinue)
        }
    }
}

/** Ronda actual, con puntos de progreso y la explicación de la ronda. */
@Composable
private fun RoundHeader(game: ForgeGameState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "RONDA ${game.roundIndex + 1} DE ${game.rounds.size}",
                color = NeonGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )
            game.rounds.forEachIndexed { index, round ->
                val done = index < game.roundIndex
                val current = index == game.roundIndex
                Box(
                    modifier = Modifier
                        .size(if (current) 22.dp else 18.dp)
                        .background(
                            when {
                                done -> GoodColor.copy(alpha = 0.25f)
                                current -> NeonGold.copy(alpha = 0.2f)
                                else -> DungeonBlack
                            },
                            CircleShape
                        )
                        .border(1.dp, if (done) GoodColor else if (current) NeonGold else DungeonBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (round.isDefend) "🦹" else "🔨", fontSize = 9.sp)
                }
            }
        }
        game.round.title?.let {
            Text(text = it, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        game.round.goal?.let {
            Text(text = it, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// ── Ronda de construir ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildRound(
    game: ForgeGameState,
    onToggleBlock: (String) -> Unit,
    onForge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editing = game.phase == ForgePhase.EDITING
    val key = game.key
    val level = KeyStrength.level(key)
    val goal = (game.round.minLevel ?: 2).coerceIn(0, KeyStrength.MAX_LEVEL)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // La llave sobre el yunque
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialColors[level].copy(alpha = if (key.isEmpty()) 0.3f else 0.9f)),
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🗝️", fontSize = 22.sp)
                    Text(
                        text = key.ifEmpty { "Tu llave está vacía" },
                        color = if (key.isEmpty()) TextMuted else TextPrimary,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${key.length} letras", color = TextMuted, fontSize = 11.sp)
                }
                if (game.selectedBlocks.isEmpty()) {
                    Text(
                        text = "Toca los bloques de abajo para armar tu llave. Toca un bloque de la llave para quitarlo.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        game.selectedBlocks.forEach { block ->
                            BlockChip(block = block, enabled = editing, inKey = true) { onToggleBlock(block.id) }
                        }
                    }
                }
            }
        }

        StrengthMeter(level = level, goal = goal, hasKey = key.isNotEmpty())

        // Consejo para endurecerla (solo mientras arma)
        val tip = KeyStrength.tips(key).firstOrNull()
        if (editing && key.isNotEmpty() && tip != null && level < KeyStrength.MAX_LEVEL) {
            Text(text = "💡 $tip", color = TextSecondary, fontSize = 12.sp)
        }

        // Bloques disponibles
        Text(
            text = "⚒️ BLOQUES (máximo ${ForgeGameState.MAX_BLOCKS})",
            color = NeonGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            game.round.blocks.orEmpty()
                .filter { it.id !in game.selectedIds }
                .forEach { block ->
                    val full = game.selectedIds.size >= ForgeGameState.MAX_BLOCKS
                    BlockChip(block = block, enabled = editing && !full, inKey = false) { onToggleBlock(block.id) }
                }
        }

        Button(
            onClick = onForge,
            enabled = editing && key.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("🔨 ¡Forjar y probar la llave!", color = DungeonBlack, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun BlockChip(block: ForgeBlock, enabled: Boolean, inKey: Boolean, onClick: () -> Unit) {
    val color = blockColor(block)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = if (inKey) 0.25f else 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = if (enabled) 0.8f else 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = block.text,
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            block.caption?.let {
                Text(text = it, color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

/** Medidor de dureza en 4 materiales, con la meta de la ronda marcada. */
@Composable
private fun StrengthMeter(level: Int, goal: Int, hasKey: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyStrength.MATERIALS.forEachIndexed { index, material ->
                val reached = hasKey && index <= level
                val color by animateColorAsState(
                    targetValue = if (reached) MaterialColors[level] else DungeonBlack,
                    label = "meterSegment"
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color)
                            .border(
                                if (index == goal) 2.dp else 0.5.dp,
                                if (index == goal) NeonGold else DungeonBorder,
                                RoundedCornerShape(5.dp)
                            )
                    )
                    Text(
                        text = "${material.emoji} ${material.name}",
                        color = if (hasKey && index == level) TextPrimary else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (hasKey && index == level) FontWeight.Black else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        val goalMaterial = KeyStrength.MATERIALS[goal]
        Text(
            text = "🎯 Meta: llave de ${goalMaterial.emoji} ${goalMaterial.name} o mejor",
            color = NeonGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Ronda de defender ─────────────────────────────────────────────────────────

@Composable
private fun DefendRound(
    game: ForgeGameState,
    onChooseOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val round = game.round
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BadColor.copy(alpha = 0.5f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(BadColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, BadColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = round.emoji ?: "🦹", fontSize = 24.sp)
                    }
                    game.forgedKeys.lastOrNull()?.let { key ->
                        Column {
                            Text("Tu llave", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = "🗝️ $key",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = round.situation.orEmpty(),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
        }

        if (game.phase == ForgePhase.DEFENDING) {
            Text("¿Qué haces?", color = NeonGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        round.options.orEmpty().forEach { option ->
            val discarded = option.id in game.disabledOptionIds
            Surface(
                onClick = { onChooseOption(option.id) },
                enabled = game.phase == ForgePhase.DEFENDING && !discarded,
                shape = RoundedCornerShape(14.dp),
                color = if (discarded) DungeonBlack.copy(alpha = 0.4f) else DungeonCard,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (discarded) DungeonBorder else WordColor.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = if (discarded) "✗" else "🛡️", fontSize = 16.sp, color = BadColor)
                    Text(
                        text = option.text,
                        color = if (discarded) TextMuted else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        textDecoration = if (discarded) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

// ── Resultado ─────────────────────────────────────────────────────────────────

@Composable
private fun ResultPanel(game: ForgeGameState, onContinue: () -> Unit) {
    val success = game.phase == ForgePhase.SUCCESS
    val color = if (success) GoodColor else BadColor
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(game.resultTitle.orEmpty(), color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(game.resultMessage.orEmpty(), color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = when {
                        !success && game.round.isDefend -> "⏪ Elegir otra vez"
                        !success -> "🔨 Arreglar mi llave"
                        game.isLastRound -> "🏆 ¡Terminar!"
                        else -> "Siguiente ronda ➜"
                    },
                    color = DungeonBlack,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
