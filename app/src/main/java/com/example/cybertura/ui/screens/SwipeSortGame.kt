package com.example.cybertura.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.SwipeCard
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.SwipeGameState
import kotlinx.coroutines.launch
import kotlin.math.abs

private val SafeColor = NeonGreen
private val TrapColor = NeonPink

/**
 * Minijuego "¿Seguro o trampa?": las cartas se deslizan a la derecha (seguro)
 * o a la izquierda (trampa). También hay botones para quien prefiera tocar.
 * Sin castigo: una carta mal clasificada vuelve al final del mazo con su explicación.
 */
@Composable
fun SwipeSortGame(
    game: SwipeGameState,
    safeLabel: String,
    trapLabel: String,
    onSwipe: (saidSafe: Boolean) -> Unit,
    onAcknowledge: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Sonido y vibración por carta (la última la celebra el modal de victoria)
    LaunchedEffect(game.swipeCount) {
        val result = game.lastResult ?: return@LaunchedEffect
        if (result.isFinal) return@LaunchedEffect
        if (result.wasCorrect) {
            SoundManager.playCorrect()
        } else {
            SoundManager.playFail()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Estado de la carta de arriba; se reinicia con cada deslizamiento
    var deckWidthPx by remember { mutableFloatStateOf(1f) }
    val threshold = deckWidthPx * 0.28f
    val offsetX = remember(game.swipeCount) { Animatable(0f) }
    var flinging by remember(game.swipeCount) { mutableStateOf(false) }
    val locked = game.awaitingAck || flinging || game.queue.isEmpty()

    fun fling(saidSafe: Boolean) {
        if (locked) return
        flinging = true
        scope.launch {
            offsetX.animateTo(if (saidSafe) deckWidthPx * 1.4f else -deckWidthPx * 1.4f, tween(220))
            onSwipe(saidSafe)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwipeHud(game)

        // Guía de lados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("← ${trapLabel.uppercase()}", color = TrapColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("${safeLabel.uppercase()} →", color = SafeColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }

        // Mazo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .onSizeChanged { deckWidthPx = it.width.toFloat().coerceAtLeast(1f) },
            contentAlignment = Alignment.Center
        ) {
            // Carta de atrás (la siguiente), asomando debajo
            game.queue.getOrNull(1)?.let { next ->
                MessageCard(
                    card = next,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 0.93f
                            scaleY = 0.93f
                            translationY = 14.dp.toPx()
                            alpha = 0.55f
                        }
                )
            }

            // Carta de arriba (arrastrable)
            game.queue.firstOrNull()?.let { top ->
                key(game.swipeCount) {
                    val drag = offsetX.value
                    MessageCard(
                        card = top,
                        dragProgress = (drag / threshold).coerceIn(-1f, 1f),
                        safeLabel = safeLabel,
                        trapLabel = trapLabel,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = drag
                                rotationZ = drag / deckWidthPx * 14f
                            }
                            .pointerInput(game.swipeCount, locked) {
                                if (locked) return@pointerInput
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX.value > threshold -> fling(saidSafe = true)
                                            offsetX.value < -threshold -> fling(saidSafe = false)
                                            else -> scope.launch {
                                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch { offsetX.animateTo(0f, spring()) }
                                    }
                                ) { change, amount ->
                                    change.consume()
                                    scope.launch { offsetX.snapTo(offsetX.value + amount) }
                                }
                            }
                    )
                }
            }
        }

        // Explicación de la última carta
        SwipeFeedback(game, safeLabel, trapLabel, onAcknowledge)

        // Botones grandes: alternativa a deslizar (y más accesible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SideButton(
                text = "✗  $trapLabel",
                color = TrapColor,
                enabled = !locked,
                modifier = Modifier.weight(1f)
            ) { fling(saidSafe = false) }
            SideButton(
                text = "✓  $safeLabel",
                color = SafeColor,
                enabled = !locked,
                modifier = Modifier.weight(1f)
            ) { fling(saidSafe = true) }
        }
    }
}

/** Progreso + racha de aciertos. */
@Composable
private fun SwipeHud(game: SwipeGameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Clasificadas ${game.sorted} de ${game.total}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            val progress by animateFloatAsState(
                targetValue = if (game.total > 0) game.sorted.toFloat() / game.total else 0f,
                label = "swipeProgress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DungeonBlack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SafeColor)
                )
            }
        }

        // Racha: aparece desde 2 aciertos seguidos y "salta" con cada nuevo acierto
        AnimatedVisibility(
            visible = game.streak >= 2,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            val pop = remember { Animatable(1f) }
            LaunchedEffect(game.streak) {
                pop.snapTo(1.35f)
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = NeonGold.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGold),
                modifier = Modifier.scale(pop.value)
            ) {
                Text(
                    text = "🔥 x${game.streak}",
                    color = NeonGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Carta con forma de mensaje recibido. [dragProgress] -1..1 tiñe la carta y muestra el sello. */
@Composable
private fun MessageCard(
    card: SwipeCard,
    modifier: Modifier = Modifier,
    dragProgress: Float = 0f,
    safeLabel: String = "",
    trapLabel: String = ""
) {
    val tint = when {
        dragProgress > 0f -> SafeColor
        dragProgress < 0f -> TrapColor
        else -> DungeonBorder
    }
    val strength = abs(dragProgress)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DungeonCard,
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (strength > 0.05f) tint.copy(alpha = 0.4f + strength * 0.6f) else DungeonBorder
        ),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.background(tint.copy(alpha = strength * 0.12f))) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Remitente
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(DungeonCardHover, CircleShape)
                            .border(1.dp, DungeonBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = card.emoji ?: "💬", fontSize = 20.sp)
                    }
                    Text(
                        text = card.sender ?: "Mensaje",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Burbuja del mensaje
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    color = DungeonBlack.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = card.text,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Desliza la carta  ←  →",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Sello "SEGURO" / "TRAMPA" que aparece al arrastrar
            if (strength > 0.05f) {
                val label = if (dragProgress > 0f) "✓ ${safeLabel.uppercase()}" else "✗ ${trapLabel.uppercase()}"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(3.dp, tint),
                    modifier = Modifier
                        .align(if (dragProgress > 0f) Alignment.TopStart else Alignment.TopEnd)
                        .padding(18.dp)
                        .graphicsLayer {
                            alpha = strength
                            rotationZ = if (dragProgress > 0f) -12f else 12f
                        }
                ) {
                    Text(
                        text = label,
                        color = tint,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Explicación de la carta recién clasificada.
 * Acierto: aviso verde que no interrumpe. Fallo: panel que bloquea el mazo hasta pulsar "Entendido".
 */
@Composable
private fun SwipeFeedback(
    game: SwipeGameState,
    safeLabel: String,
    trapLabel: String,
    onAcknowledge: () -> Unit
) {
    val result = game.lastResult
    AnimatedVisibility(
        visible = result != null && !result.isFinal,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut()
    ) {
        if (result == null) return@AnimatedVisibility
        val color = if (result.wasCorrect) SafeColor else TrapColor
        val correctLabel = if (result.card.isSafe) safeLabel else trapLabel
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (result.wasCorrect) "✓ ¡Bien! Era $correctLabel" else "¡Ojo! Esta era $correctLabel",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = result.card.feedback,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (game.awaitingAck) {
                    Text(
                        text = "Volverá a aparecer más adelante para que la intentes de nuevo.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = onAcknowledge,
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Entendido", color = DungeonBlack, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SideButton(
    text: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (enabled) color else color.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier.height(52.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) color else color.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}
