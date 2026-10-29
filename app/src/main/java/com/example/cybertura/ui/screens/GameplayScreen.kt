package com.example.cybertura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.Option
import com.example.cybertura.ui.components.FeedbackModal
import com.example.cybertura.ui.components.FloatingParticles
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.GameplayUiState

@Composable
fun GameplayScreen(
    uiState: GameplayUiState,
    onOptionSelected: (Option) -> Unit,
    onDismissFeedback: () -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateResult: () -> Unit,
    onBackClick: () -> Unit,
    onSwipe: (saidSafe: Boolean) -> Unit = {},
    onAcknowledgeSwipe: () -> Unit = {},
    onTapScene: (clueId: String?) -> Unit = {},
    onUseHint: () -> Unit = {},
    onChooseChatReply: (choiceId: String) -> Unit = {},
    onRetryChatReply: () -> Unit = {},
    onToggleForgeBlock: (blockId: String) -> Unit = {},
    onForgeKey: () -> Unit = {},
    onChooseForgeOption: (optionId: String) -> Unit = {},
    onContinueForge: () -> Unit = {},
    onStartArcadeWave: () -> Unit = {},
    onArcadeWaveEnded: (shieldsLost: Int, failed: Boolean) -> Unit = { _, _ -> }
) {
    val challenge = uiState.challenge
    val module = uiState.module

    val accent = try {
        Color(android.graphics.Color.parseColor(module?.accentColor ?: "#00F5D4"))
    } catch (e: Exception) {
        NeonCyan
    }

    Scaffold(
        topBar = {
            // Battle HUD
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(DungeonBlack, DungeonDark.copy(alpha = 0.9f))
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(DungeonCardHover, CircleShape)
                                    .border(1.dp, DungeonBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Salir",
                                    tint = NeonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = module?.title?.uppercase() ?: "MISIÓN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accent,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Reto ${uiState.challengeIndex} de ${uiState.totalChallenges}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }

                            // XP reward chip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NeonGold.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, NeonGold.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "✦", fontSize = 12.sp, color = NeonGold)
                                    Text(
                                        text = "+${challenge?.points ?: 100}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGold
                                    )
                                }
                            }
                        }

                        // Battle progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DungeonBlack)
                                .border(0.5.dp, DungeonBorder, RoundedCornerShape(3.dp))
                        ) {
                            val progress = if (uiState.totalChallenges > 0) {
                                uiState.challengeIndex.toFloat() / uiState.totalChallenges.toFloat()
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accent.copy(alpha = 0.6f), accent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        },
        containerColor = DungeonDark
    ) { innerPadding ->
        if (challenge != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingParticles(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    particleColor = accent.copy(alpha = 0.05f),
                    count = 8
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // ─── Encounter Story / Situación ───────────
                    if (challenge.storyContext.isNotBlank()) {
                        Surface(
                            color = DungeonCard,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, accent.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accent.copy(alpha = 0.04f), Color.Transparent)
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(accent.copy(alpha = 0.2f), CircleShape)
                                                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "📖", fontSize = 12.sp)
                                        }
                                        Text(
                                            text = "BRIEFING DE MISIÓN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = accent,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                    Text(
                                        text = challenge.storyContext,
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        lineHeight = 21.sp
                                    )
                                }
                            }
                        }
                    }

                    // ─── Question Card ─────────────────────────
                    val infiniteTransition = rememberInfiniteTransition(label = "questionGlow")
                    val questionGlow by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "qGlow"
                    )

                    Surface(
                        color = DungeonCard,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRoundRect(
                                    color = accent.copy(alpha = questionGlow * 0.15f),
                                    cornerRadius = CornerRadius(20.dp.toPx()),
                                    size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx())
                                )
                            }
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        accent.copy(alpha = questionGlow + 0.1f),
                                        accent.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Help,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "DESAFÍO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accent,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Text(
                                text = challenge.question,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // ─── Minijuegos ────────────────────────────
                    val swipe = uiState.swipe
                    val spot = uiState.spot
                    val scene = challenge.scene
                    val chat = uiState.chat
                    val forge = uiState.forge
                    val arcade = uiState.arcade
                    if (swipe != null) {
                        SwipeSortGame(
                            game = swipe,
                            safeLabel = challenge.safeLabel ?: "Seguro",
                            trapLabel = challenge.trapLabel ?: "Trampa",
                            onSwipe = onSwipe,
                            onAcknowledge = onAcknowledgeSwipe
                        )
                    } else if (spot != null && scene != null) {
                        SpotCluesGame(
                            game = spot,
                            scene = scene,
                            onTap = onTapScene,
                            onHint = onUseHint
                        )
                    } else if (chat != null) {
                        ChatStoryGame(
                            game = chat,
                            onChoose = onChooseChatReply,
                            onRetry = onRetryChatReply
                        )
                    } else if (forge != null) {
                        ForgeKeyGame(
                            game = forge,
                            onToggleBlock = onToggleForgeBlock,
                            onForge = onForgeKey,
                            onChooseOption = onChooseForgeOption,
                            onContinue = onContinueForge
                        )
                    } else if (arcade != null) {
                        ArcadeDefenseGame(
                            game = arcade,
                            onStartWave = onStartArcadeWave,
                            onWaveEnded = onArcadeWaveEnded
                        )
                    } else {
                        // ─── Options ───────────────────────────────
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accent, CircleShape)
                            )
                            Text(
                                text = "⚔️ ELIGE TU ACCIÓN:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = accent,
                                letterSpacing = 1.sp
                            )
                        }

                        challenge.options.forEachIndexed { index, option ->
                            val optionLabel = when (index) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "${index + 1}"
                            }

                            BattleOptionCard(
                                label = optionLabel,
                                text = option.text,
                                accentColor = accent,
                                onClick = { onOptionSelected(option) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Sonido de acierto / fallo al responder
    LaunchedEffect(uiState.showFeedback) {
        if (uiState.showFeedback) {
            if (uiState.isCorrect) SoundManager.playCorrect() else SoundManager.playFail()
        }
    }

    // Modal de retroalimentación pedagógica RPG
    if (uiState.showFeedback) {
        FeedbackModal(
            isCorrect = uiState.isCorrect,
            title = uiState.resultTitle
                ?: if (uiState.isCorrect) "¡Golpe Crítico de Sabiduría!" else "¡El Escudo Resistió!",
            feedbackText = uiState.feedbackText,
            educationalTip = uiState.educationalTip,
            pointsAwarded = uiState.challenge?.points ?: 100,
            onContinue = {
                onDismissFeedback()
                if (uiState.isCorrect) {
                    if (uiState.isModuleFinished) {
                        onNavigateResult()
                    } else {
                        onNavigateNext()
                    }
                }
            }
        )
    }
}

@Composable
private fun BattleOptionCard(
    label: String,
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "optionHover")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "optBorder"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = DungeonCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                accentColor.copy(alpha = borderAlpha),
                RoundedCornerShape(16.dp)
            )
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.03f), Color.Transparent)
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hexagonal-like option badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.25f),
                                    DungeonBlack
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            accentColor.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }

                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
