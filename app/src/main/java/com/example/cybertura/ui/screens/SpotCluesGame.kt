package com.example.cybertura.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.ClueScene
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.SpotGameState
import kotlinx.coroutines.delay

private const val CLUE_TAG = "clue"
private val ClueMarkup = Regex("""\[\[(\w+)\|(.+?)]]""")
private val FoundColor = NeonGold

/**
 * Minijuego del detective: el niño toca las partes sospechosas de una escena
 * (correo, chat, publicación…). Las pistas encontradas se resaltan y se explican.
 */
@Composable
fun SpotCluesGame(
    game: SpotGameState,
    scene: ClueScene,
    onTap: (clueId: String?) -> Unit,
    onHint: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    var showMissMessage by remember { mutableStateOf(false) }

    // Reacción a cada toque: sonido al encontrar, vibración + sacudida al fallar
    LaunchedEffect(game.eventCount) {
        val event = game.lastEvent ?: return@LaunchedEffect
        if (event.clueId != null) {
            showMissMessage = false
            if (!event.isFinal) SoundManager.playCorrect()
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showMissMessage = true
            for (x in listOf(14f, -12f, 8f, -5f, 0f)) shake.animateTo(x, tween(45))
            delay(1400)
            showMissMessage = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpotHud(game, onHint)

        // Ayuda activa
        val hintClue = game.hintClueId?.let { id -> game.clues.firstOrNull { it.id == id } }
        AnimatedVisibility(visible = hintClue != null, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
            if (hintClue == null) return@AnimatedVisibility
            InfoBanner(
                color = NeonCyan,
                text = "💡 ${hintClue.hint ?: "Busca algo relacionado con: ${hintClue.label}"}"
            )
        }

        AnimatedVisibility(visible = showMissMessage, enter = fadeIn(), exit = fadeOut()) {
            InfoBanner(color = TextMuted, text = "🤔 Eso parece normal. ¡Sigue buscando!")
        }

        // Escena a inspeccionar
        SceneCard(
            scene = scene,
            foundIds = game.foundIds,
            onTap = onTap,
            modifier = Modifier.graphicsLayer { translationX = shake.value }
        )

        FoundCluesList(game)
    }
}

/** Progreso de pistas + botón de ayuda. */
@Composable
private fun SpotHud(game: SpotGameState, onHint: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "🔍 Pistas encontradas: ${game.foundIds.size} de ${game.total}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            val progress by animateFloatAsState(
                targetValue = if (game.total > 0) game.foundIds.size.toFloat() / game.total else 0f,
                label = "spotProgress"
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
                        .background(FoundColor)
                )
            }
        }

        val canHint = game.hintClueId == null && game.foundIds.size < game.total
        OutlinedButton(
            onClick = onHint,
            enabled = canHint,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = if (canHint) 0.8f else 0.25f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("💡 Ayuda", color = NeonCyan.copy(alpha = if (canHint) 1f else 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun InfoBanner(color: Color, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/** La "pantalla" de la app donde ocurre la escena: barra superior, título, líneas y botón. */
@Composable
private fun SceneCard(
    scene: ClueScene,
    foundIds: List<String>,
    onTap: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DungeonCard,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, DungeonBorder),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Barra de la app
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DungeonBlack)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = scene.emoji ?: "📱", fontSize = 18.sp)
                Text(
                    text = scene.app ?: "Mensaje",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "• • •", color = TextMuted, fontSize = 12.sp)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                scene.title?.let {
                    ClueText(markup = it, foundIds = foundIds, onTap = onTap, fontSize = 17.sp, bold = true)
                }
                scene.lines.orEmpty().forEach { line ->
                    ClueText(markup = line, foundIds = foundIds, onTap = onTap, fontSize = 15.sp)
                }
                scene.button?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonBlue.copy(alpha = 0.85f))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ClueText(
                            markup = it, foundIds = foundIds, onTap = onTap,
                            fontSize = 15.sp, bold = true, color = Color.White, center = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Texto con pistas escondidas. Las pistas no se distinguen hasta encontrarlas;
 * entonces se resaltan en dorado. Un toque fuera de una pista cuenta como fallo.
 */
@Composable
private fun ClueText(
    markup: String,
    foundIds: List<String>,
    onTap: (String?) -> Unit,
    fontSize: TextUnit,
    bold: Boolean = false,
    color: Color = TextPrimary,
    center: Boolean = false
) {
    val text = remember(markup, foundIds) { buildClueText(markup, foundIds) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.45f,
        fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(text) {
                detectTapGestures { position ->
                    val l = layout ?: return@detectTapGestures
                    val offset = l.getOffsetForPosition(position)
                    // Tolerancia de un carácter a cada lado: los dedos de los niños no son precisos
                    val hit = text.getStringAnnotations(
                        CLUE_TAG,
                        (offset - 1).coerceAtLeast(0),
                        (offset + 1).coerceAtMost(text.length)
                    ).firstOrNull()
                    currentOnTap(hit?.item)
                }
            }
    )
}

private fun buildClueText(markup: String, foundIds: List<String>): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in ClueMarkup.findAll(markup)) {
        append(markup.substring(last, match.range.first))
        val id = match.groupValues[1]
        val visible = match.groupValues[2]
        val start = length
        if (id in foundIds) {
            withStyle(SpanStyle(background = FoundColor.copy(alpha = 0.35f), color = FoundColor, fontWeight = FontWeight.Black)) {
                append(visible)
            }
        } else {
            append(visible)
        }
        addStringAnnotation(CLUE_TAG, id, start, length)
        last = match.range.last + 1
    }
    append(markup.substring(last))
}

/** Libreta del detective: pistas encontradas con su explicación y huecos por descubrir. */
@Composable
private fun FoundCluesList(game: SpotGameState) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DungeonBlack.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📒 LIBRETA DEL DETECTIVE",
                color = FoundColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            game.foundIds.forEachIndexed { index, id ->
                val clue = game.clues.firstOrNull { it.id == id } ?: return@forEachIndexed
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(FoundColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.5.dp, FoundColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = FoundColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(clue.label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Text(clue.explanation, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
            repeat(game.total - game.foundIds.size) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .border(1.5.dp, DungeonBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Text("Pista sin descubrir…", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
