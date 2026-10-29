package com.example.cybertura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.cybertura.data.model.Challenge
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.ui.components.*
import com.example.cybertura.ui.theme.*

@Composable
fun ModuleDetailScreen(
    module: CyberModule,
    userProgress: UserProgress,
    onBackClick: () -> Unit,
    onBadgesClick: () -> Unit,
    onChallengeClick: (String) -> Unit
) {
    val accent = try {
        Color(android.graphics.Color.parseColor(module.accentColor))
    } catch (e: Exception) {
        NeonCyan
    }

    val isModuleCompleted = userProgress.isModuleCompleted(module.id)
    val nextChallenge = module.challenges.firstOrNull { !userProgress.isChallengeCompleted(it.id) }
        ?: module.challenges.firstOrNull()

    val completedCount = userProgress.getCompletedChallengesCount(module)

    Scaffold(
        topBar = {
            CyberTopBar(
                agentName = userProgress.agentName,
                points = userProgress.totalScore,
                onBadgesClick = onBadgesClick,
                showBack = true,
                onBackClick = onBackClick,
                progress = userProgress
            )
        },
        containerColor = DungeonDark,
        bottomBar = {
            if (nextChallenge != null) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, DungeonBlack.copy(alpha = 0.95f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "btnGlow")
                        val btnGlow by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "btnPulse"
                        )

                        Button(
                            onClick = { onChallengeClick(nextChallenge.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = accent.copy(alpha = btnGlow * 0.3f),
                                        cornerRadius = CornerRadius(16.dp.toPx()),
                                        size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                                        topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                                    )
                                }
                        ) {
                            Icon(
                                imageVector = if (isModuleCompleted) Icons.Default.Replay else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = DungeonBlack,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isModuleCompleted) "⚔️ Repasar Misión" else "⚔️ ¡Iniciar Combate!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = DungeonBlack
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingParticles(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                particleColor = accent.copy(alpha = 0.06f)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── Dungeon Header ────────────────────────────
                item {
                    DungeonHeader(
                        module = module,
                        accent = accent,
                        isCompleted = isModuleCompleted,
                        completedCount = completedCount
                    )
                }

                // ─── Quest Log Title ───────────────────────────
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(accent, CircleShape)
                        )
                        Text(
                            text = "📜 REGISTRO DE RETOS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = accent,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // ─── Challenge List ────────────────────────────
                itemsIndexed(module.challenges) { index, challenge ->
                    val isCompleted = userProgress.isChallengeCompleted(challenge.id)
                    val isNext = challenge.id == nextChallenge?.id

                    QuestLogItem(
                        index = index + 1,
                        challenge = challenge,
                        isCompleted = isCompleted,
                        isNext = isNext,
                        accentColor = accent,
                        onClick = { onChallengeClick(challenge.id) }
                    )
                }

                // Space for bottom bar
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun DungeonHeader(
    module: CyberModule,
    accent: Color,
    isCompleted: Boolean,
    completedCount: Int
) {
    PulsingGlowBox(
        glowColor = accent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, accent.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 500f
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CyberModuleIcon(
                            iconName = module.icon,
                            accentColor = accent,
                            isCompleted = isCompleted,
                            modifier = Modifier.size(64.dp)
                        )
                        Column {
                            if (isCompleted) {
                                Surface(
                                    color = NeonGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "✅ MISIÓN COMPLETADA",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = module.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = module.subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                    }

                    Text(
                        text = module.description,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    // Quest progress
                    XpBar(
                        currentXp = completedCount,
                        maxXp = module.challenges.size,
                        level = completedCount,
                        showLevel = false,
                        barColor = accent,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$completedCount / ${module.challenges.size} retos superados",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "+${module.challenges.sumOf { it.points }} XP posibles",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGold
                        )
                    }

                    // Loot reward preview
                    Surface(
                        color = DungeonBlack.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, NeonGold.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonGold.copy(alpha = 0.15f))
                                    .border(1.dp, NeonGold.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🏆", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "RECOMPENSA DE MISIÓN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonGold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = module.badgeTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = module.badgeDescription,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestLogItem(
    index: Int,
    challenge: Challenge,
    isCompleted: Boolean,
    isNext: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "questItem")
    val nextGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nextItemGlow"
    )

    val borderColor = when {
        isCompleted -> NeonGold.copy(alpha = 0.5f)
        isNext -> accentColor.copy(alpha = nextGlow + 0.2f)
        else -> DungeonBorder
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = DungeonCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier.background(
                if (isNext) Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.05f), Color.Transparent)
                ) else Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Transparent)
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
                // Status orb
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Brush.radialGradient(
                                    listOf(NeonGold.copy(alpha = 0.3f), NeonGold.copy(alpha = 0.1f))
                                )
                                isNext -> Brush.radialGradient(
                                    listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.1f))
                                )
                                else -> Brush.radialGradient(
                                    listOf(DungeonCardHover, DungeonBlack)
                                )
                            }
                        )
                        .border(
                            1.5.dp,
                            when {
                                isCompleted -> NeonGold.copy(alpha = 0.6f)
                                isNext -> accentColor.copy(alpha = 0.5f)
                                else -> DungeonBorder
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isCompleted -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = NeonGold,
                            modifier = Modifier.size(20.dp)
                        )
                        isNext -> Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Siguiente",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        else -> Text(
                            text = "$index",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCompleted || isNext) TextPrimary else TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✦ +${challenge.points} XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) NeonGold else TextMuted
                        )
                        if (isNext) {
                            Surface(
                                color = accentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SIGUIENTE",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isNext) accentColor else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
