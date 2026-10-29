package com.example.cybertura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.ui.components.FloatingParticles
import com.example.cybertura.ui.theme.*
import kotlin.math.sin

@Composable
fun ModuleResultScreen(
    module: CyberModule?,
    onGoHome: () -> Unit,
    onViewBadges: () -> Unit
) {
    val accent = try {
        Color(android.graphics.Color.parseColor(module?.accentColor ?: "#00F5D4"))
    } catch (e: Exception) {
        NeonCyan
    }

    // Fanfarria de módulo completado (una sola vez al entrar a la pantalla)
    LaunchedEffect(Unit) {
        SoundManager.playModuleCompleted()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victoryAnim")

    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyPulse"
    )

    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraRotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "victoryGlow"
    )

    Scaffold(containerColor = DungeonBlack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Epic victory particles
            FloatingParticles(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                particleColor = NeonGold.copy(alpha = 0.15f),
                count = 20
            )

            // Secondary particle layer
            FloatingParticles(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                particleColor = accent.copy(alpha = 0.1f),
                count = 10
            )

            // Radial light rays from center
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val centerX = size.width / 2
                val centerY = size.height * 0.3f
                val maxRadius = size.width * 0.8f

                // Multiple glow rings
                for (i in 0..3) {
                    val radius = maxRadius * (0.3f + i * 0.2f)
                    val alpha = (0.08f - i * 0.015f).coerceAtLeast(0.01f) * glowAlpha
                    drawCircle(
                        color = NeonGold.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Epic "VICTORY" header
                    Text(
                        text = "⚔️ VICTORIA ⚔️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGold.copy(alpha = glowAlpha),
                        letterSpacing = 4.sp
                    )

                    // Trophy with glow aura
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .drawBehind {
                                // Multi-ring aura
                                drawCircle(
                                    color = NeonGold.copy(alpha = glowAlpha * 0.1f),
                                    radius = size.minDimension * 0.9f
                                )
                                drawCircle(
                                    color = NeonGold.copy(alpha = glowAlpha * 0.15f),
                                    radius = size.minDimension * 0.7f
                                )
                                drawCircle(
                                    color = NeonGold.copy(alpha = glowAlpha * 0.2f),
                                    radius = size.minDimension * 0.55f
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((100 * trophyScale).dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            NeonGold.copy(alpha = 0.4f),
                                            NeonAmber.copy(alpha = 0.2f),
                                            DungeonBlack
                                        )
                                    )
                                )
                                .border(
                                    3.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            NeonGold.copy(alpha = glowAlpha + 0.2f),
                                            NeonAmber.copy(alpha = 0.5f),
                                            NeonGold.copy(alpha = glowAlpha)
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏆", fontSize = 48.sp)
                        }
                    }

                    Text(
                        text = "¡MISIÓN COMPLETADA!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "¡Felicitaciones, Agente! Has protegido\neste sector digital con éxito.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    // Loot Card
                    Surface(
                        color = DungeonCard,
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    NeonGold.copy(alpha = glowAlpha),
                                    NeonAmber.copy(alpha = 0.3f),
                                    NeonGold.copy(alpha = glowAlpha)
                                )
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.background(
                                Brush.radialGradient(
                                    listOf(
                                        NeonGold.copy(alpha = 0.06f),
                                        Color.Transparent
                                    ),
                                    radius = 400f
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = RarityLegendary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "★ RECOMPENSA LEGENDARIA ★",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGold,
                                        letterSpacing = 1.5.sp
                                    )
                                }

                                Text(
                                    text = module?.badgeTitle ?: "Insignia Cybertura",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = module?.badgeDescription ?: "Has demostrado gran sabiduría cibernética.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onGoHome,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = DungeonBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🗺️ Volver al Mapa",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = DungeonBlack
                        )
                    }

                    OutlinedButton(
                        onClick = onViewBadges,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, NeonGold.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(text = "🏆", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ver mi Inventario de Trofeos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGold
                        )
                    }
                }
            }
        }
    }
}
