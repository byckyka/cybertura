package com.example.cybertura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.ui.components.FloatingParticles
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.BadgeItem
import com.example.cybertura.ui.viewmodel.BadgesUiState

@Composable
fun BadgesScreen(
    uiState: BadgesUiState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(DungeonBlack, DungeonDark)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🏆 SALA DE TROFEOS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "${uiState.unlockedCount} de ${uiState.badges.size} Desbloqueados",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(modifier = Modifier.size(34.dp))
                    }
                }
            }
        },
        containerColor = DungeonDark
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingParticles(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                particleColor = NeonGold.copy(alpha = 0.06f),
                count = 8
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Intro card
                item {
                    Surface(
                        color = DungeonCard,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, NeonGold.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            NeonGold.copy(alpha = 0.05f),
                                            Color.Transparent
                                        ),
                                        radius = 400f
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
                                            .size(6.dp)
                                            .background(NeonGold, CircleShape)
                                    )
                                    Text(
                                        text = "INVENTARIO DE LOGROS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Text(
                                    text = "Completa cada misión para desbloquear trofeos legendarios y demostrar tu maestría en ciberseguridad.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )

                                // Total XP display
                                Surface(
                                    color = DungeonBlack.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${uiState.totalScore}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeonGold
                                            )
                                            Text(
                                                text = "XP Total",
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${uiState.unlockedCount}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeonCyan
                                            )
                                            Text(
                                                text = "Trofeos",
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Badge items
                items(uiState.badges) { badge ->
                    TrophyItem(badge = badge)
                }
            }
        }
    }
}

@Composable
private fun TrophyItem(badge: BadgeItem) {
    val isUnlocked = badge.isUnlocked

    val infiniteTransition = rememberInfiniteTransition(label = "trophyGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyItemGlow"
    )

    val borderBrush = if (isUnlocked) {
        Brush.linearGradient(
            listOf(
                NeonGold.copy(alpha = glowAlpha + 0.2f),
                NeonAmber.copy(alpha = glowAlpha),
                NeonGold.copy(alpha = glowAlpha + 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(DungeonBorder, DungeonBorder)
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DungeonCard,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUnlocked) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = NeonGold.copy(alpha = glowAlpha * 0.1f),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier.background(
                if (isUnlocked) {
                    Brush.horizontalGradient(
                        listOf(NeonGold.copy(alpha = 0.04f), Color.Transparent)
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trophy orb
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) {
                                Brush.radialGradient(
                                    listOf(
                                        NeonGold.copy(alpha = 0.3f),
                                        NeonGold.copy(alpha = 0.1f),
                                        DungeonBlack
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(DungeonCardHover, DungeonBlack)
                                )
                            }
                        )
                        .border(
                            2.dp,
                            if (isUnlocked) NeonGold.copy(alpha = 0.6f) else DungeonBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Text(text = "🏆", fontSize = 26.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Module origin
                    Text(
                        text = badge.moduleTitle.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUnlocked) NeonCyan else TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Badge name
                    Text(
                        text = badge.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUnlocked) TextPrimary else TextMuted
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Description
                    Text(
                        text = if (isUnlocked) badge.description else "🔒 Completa la misión para desbloquear",
                        fontSize = 12.sp,
                        color = if (isUnlocked) TextSecondary else TextMuted,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )

                    // Rarity tag
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = RarityLegendary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "★ LEGENDARIO",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
