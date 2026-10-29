package com.example.cybertura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.ui.components.*
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onModuleClick: (String) -> Unit,
    onBadgesClick: () -> Unit,
    onResetProgress: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CyberTopBar(
                agentName = uiState.progress.agentName,
                points = uiState.progress.totalScore,
                onBadgesClick = onBadgesClick,
                progress = uiState.progress
            )
        },
        containerColor = DungeonDark
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background particles
            FloatingParticles(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                particleColor = NeonCyan.copy(alpha = 0.08f)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── Character Status Card ─────────────────────
                item {
                    CharacterStatusCard(
                        progress = uiState.progress,
                        unlockedBadges = uiState.unlockedBadgesCount,
                        totalBadges = uiState.totalBadgesCount
                    )
                }

                // ─── Section Title ─────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeonCyan, CircleShape)
                            )
                            Text(
                                text = "⚔️ MISIONES DISPONIBLES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan,
                                letterSpacing = 1.5.sp
                            )
                        }

                        TextButton(
                            onClick = { showResetDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // ─── Quest Cards (Módulos) ─────────────────────
                itemsIndexed(uiState.modules) { index, module ->
                    val progressPercent = uiState.progress.getModuleProgressPercentage(module)
                    val completedCount = uiState.progress.getCompletedChallengesCount(module)
                    val isCompleted = uiState.progress.isModuleCompleted(module.id)

                    QuestCard(
                        module = module,
                        questNumber = index + 1,
                        progressPercent = progressPercent,
                        completedChallenges = completedCount,
                        totalChallenges = module.challenges.size,
                        isCompleted = isCompleted,
                        onClick = { onModuleClick(module.id) }
                    )
                }

                // Extra space at bottom
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = NeonAmber)
                    Text(
                        text = "¿Reiniciar aventura?",
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Tu progreso, nivel y medallas volverán a cero para explorar desde el inicio. Ideal para pruebas del prototipo.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Text("⚡ Reiniciar", fontWeight = FontWeight.Bold, color = DungeonBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DungeonCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CHARACTER STATUS CARD — Panel de personaje tipo RPG
// ═══════════════════════════════════════════════════════════════
@Composable
private fun CharacterStatusCard(
    progress: com.example.cybertura.data.model.UserProgress,
    unlockedBadges: Int,
    totalBadges: Int
) {
    PulsingGlowBox(
        glowColor = NeonCyan,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, NeonCyan.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            radius = 500f,
                            center = Offset(100f, 50f)
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header con nombre del gremio
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(NeonCyan, NeonCyan.copy(alpha = 0.3f))
                                    ),
                                    CircleShape
                                )
                        )
                        Text(
                            text = "ACADEMIA CYBERTURA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // Nombre épico y rango
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "¡Hola, ${progress.agentName}!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "Rango: ${progress.rankTitle}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                    }

                    // XP Bar
                    XpBar(
                        currentXp = progress.currentXp,
                        maxXp = progress.xpToNextLevel,
                        level = progress.level,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stats Row — estilo inventario RPG
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RpgStatBadge(
                            label = "Nivel",
                            value = "${progress.level}",
                            icon = "⚡",
                            accentColor = NeonCyan
                        )
                        RpgStatBadge(
                            label = "XP Total",
                            value = "${progress.totalScore}",
                            icon = "✦",
                            accentColor = NeonGold
                        )
                        RpgStatBadge(
                            label = "Trofeos",
                            value = "$unlockedBadges/$totalBadges",
                            icon = "🏆",
                            accentColor = NeonAmber
                        )
                        RpgStatBadge(
                            label = "Misiones",
                            value = "${progress.completedModuleIds.size}",
                            icon = "⚔️",
                            accentColor = NeonPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RpgStatBadge(
    label: String,
    value: String,
    icon: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// QUEST CARD — Tarjeta de Misión estilo RPG con rareza
// ═══════════════════════════════════════════════════════════════
@Composable
private fun QuestCard(
    module: CyberModule,
    questNumber: Int,
    progressPercent: Float,
    completedChallenges: Int,
    totalChallenges: Int,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val accent = try {
        Color(android.graphics.Color.parseColor(module.accentColor))
    } catch (e: Exception) {
        NeonCyan
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "questProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "questGlow")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "questBorderGlow"
    )

    val borderBrush = if (isCompleted) {
        Brush.linearGradient(
            listOf(
                NeonGold.copy(alpha = borderGlow + 0.2f),
                NeonAmber.copy(alpha = borderGlow),
                NeonGold.copy(alpha = borderGlow + 0.2f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = borderGlow * 0.5f),
                accent.copy(alpha = 0.1f)
            )
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = DungeonCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCompleted) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CyberModuleIcon(
                        iconName = module.icon,
                        accentColor = accent,
                        isCompleted = isCompleted,
                        modifier = Modifier.size(56.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        // Quest number and status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, accent.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "MISIÓN $questNumber",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accent,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (isCompleted) {
                                Surface(
                                    color = NeonGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonGold,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "COMPLETADA",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonGold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = module.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = module.subtitle,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            maxLines = 2
                        )
                    }
                }

                // Quest progress bar
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.Star else Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (isCompleted) NeonGold else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isCompleted) "¡Misión Completada!" else "$completedChallenges / $totalChallenges retos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) NeonGold else TextMuted
                            )
                        }
                        Text(
                            text = "${(progressPercent * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = accent
                        )
                    }

                    // Styled HP-bar-like progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DungeonBlack)
                            .border(0.5.dp, DungeonBorder, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accent.copy(alpha = 0.6f), accent)
                                    )
                                )
                        )
                    }

                    // Reward preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🏅",
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Recompensa: ${module.badgeTitle}",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
