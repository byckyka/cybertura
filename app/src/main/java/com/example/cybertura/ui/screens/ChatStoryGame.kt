package com.example.cybertura.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.ChatGameState
import com.example.cybertura.ui.viewmodel.ChatLine
import com.example.cybertura.ui.viewmodel.ChatLineKind
import com.example.cybertura.ui.viewmodel.ChatPhase

private val PlayerBubble = NeonCyan
private val GoodColor = NeonGreen
private val BadColor = NeonPink
private val NameColors = listOf(NeonGold, NeonPurple, NeonAmber, NeonBlue, NeonPink, NeonGreen)

private fun nameColor(name: String): Color = NameColors[(name.hashCode() and 0x7FFFFFFF) % NameColors.size]

/**
 * Minijuego "Chat de grupo": una conversación que avanza sola y en la que el niño
 * decide qué responder. Las malas decisiones se explican y se puede volver a elegir.
 */
@Composable
fun ChatStoryGame(
    game: ChatGameState,
    onChoose: (choiceId: String) -> Unit,
    onRetry: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Sonidos: cada mensaje que llega y cada buena decisión
    LaunchedEffect(game.lines.size) {
        when (game.lines.lastOrNull()?.kind) {
            ChatLineKind.OTHER -> SoundManager.playChat()
            ChatLineKind.GOOD_TIP -> SoundManager.playCorrect()
            else -> Unit
        }
    }
    LaunchedEffect(game.phase) {
        if (game.phase == ChatPhase.RETRY) {
            SoundManager.playFail()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChatHeader(game)
        ChatWindow(game)
        ChatControls(game, onChoose, onRetry)
    }
}

/** Barra del grupo + medidor que reacciona a las decisiones. */
@Composable
private fun ChatHeader(game: ChatGameState) {
    val story = game.story
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DungeonCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = story.emoji ?: "💬", fontSize = 20.sp)
                Text(
                    text = story.groupName ?: "Chat de grupo",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "• • •", color = TextMuted, fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = story.meterLabel ?: "Cómo se siente",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                game.meterIcons.forEachIndexed { index, icon ->
                    val active = index == game.meter
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.35f else 0.85f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "meterIcon"
                    )
                    Text(
                        text = icon,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .scale(scale)
                            .alpha(if (active) 1f else 0.3f)
                    )
                }
            }
        }
    }
}

/** Ventana de mensajes con desplazamiento automático al último. */
@Composable
private fun ChatWindow(game: ChatGameState) {
    val scroll = rememberScrollState()
    LaunchedEffect(game.lines.size, game.typingFrom) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DungeonBlack)
            .border(1.dp, DungeonBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            game.lines.forEach { line ->
                key(line.id) { AppearingLine(line) }
            }
            game.typingFrom?.let { TypingBubble(it) }
        }
    }
}

/** Cada línea entra con un pequeño deslizamiento hacia arriba. */
@Composable
private fun AppearingLine(line: ChatLine) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(250)) }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = appear.value
            translationY = (1f - appear.value) * 24f
        }
    ) {
        when (line.kind) {
            ChatLineKind.OTHER -> OtherBubble(line)
            ChatLineKind.PLAYER -> PlayerMessage(line)
            ChatLineKind.NOTE -> SystemNote(line.text)
            ChatLineKind.GOOD_TIP -> GoodTip(line.text)
        }
    }
}

@Composable
private fun Avatar(emoji: String?) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(DungeonCardHover, CircleShape)
            .border(1.dp, DungeonBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji ?: "🙂", fontSize = 16.sp)
    }
}

@Composable
private fun OtherBubble(line: ChatLine) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        Avatar(line.emoji)
        Column(modifier = Modifier.weight(1f, fill = false).widthIn(max = 260.dp)) {
            Text(
                text = line.from ?: "",
                color = nameColor(line.from ?: ""),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = DungeonCard
            ) {
                Text(
                    text = line.text,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerMessage(line: ChatLine) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 270.dp)) {
            Text(text = "Tú", color = PlayerBubble, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = PlayerBubble.copy(alpha = if (line.retracted) 0.25f else 0.9f),
            ) {
                Text(
                    text = line.text,
                    color = if (line.retracted) TextMuted else DungeonBlack,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (line.retracted) TextDecoration.LineThrough else null,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SystemNote(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(50), color = DungeonCardHover) {
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun GoodTip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GoodColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoodColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "✓ $text",
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** Burbuja "Fulano está escribiendo…" con puntitos animados. */
@Composable
private fun TypingBubble(from: String) {
    val transition = rememberInfiniteTransition(label = "typing")
    val dots by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "dots"
    )
    Text(
        text = "$from está escribiendo" + ".".repeat(dots.toInt() + 1),
        color = nameColor(from).copy(alpha = 0.85f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 40.dp)
    )
}

/** Opciones de respuesta, explicación de un error o espera mientras habla el grupo. */
@Composable
private fun ChatControls(game: ChatGameState, onChoose: (String) -> Unit, onRetry: () -> Unit) {
    when (game.phase) {
        ChatPhase.CHOOSING -> {
            val step = game.currentStep ?: return
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = step.prompt ?: "¿Qué respondes?",
                    color = NeonGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                val choices = step.choices.orEmpty().sortedBy { game.choiceOrder.indexOf(it.id) }
                choices.forEach { choice ->
                    val discarded = choice.id in game.disabledChoiceIds
                    Surface(
                        onClick = { onChoose(choice.id) },
                        enabled = !discarded,
                        shape = RoundedCornerShape(14.dp),
                        color = if (discarded) DungeonBlack.copy(alpha = 0.4f) else DungeonCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (discarded) DungeonBorder else PlayerBubble.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = if (discarded) "✗" else "💬", fontSize = 16.sp, color = BadColor)
                            Text(
                                text = choice.text,
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

        ChatPhase.RETRY -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BadColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BadColor.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¡Uy! Esa respuesta no ayuda", color = BadColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(game.retryFeedback.orEmpty(), color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = BadColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("⏪ Elegir otra vez", color = DungeonBlack, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        ChatPhase.PLAYING -> Text(
            text = "La conversación continúa…",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        ChatPhase.DONE -> Unit
    }
}
