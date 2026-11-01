package com.example.cybertura.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameCompletedScreen(
    userProgress: UserProgress,
    onShowCredits: () -> Unit,
    onFinish: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "endingAnim")
    
    // Animación de halo dorado
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    // Animación de entrada de la copa
    var showCup by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Música de juego completado (el ambiente se silencia desde el NavHost)
        SoundManager.playGameCompleted()
        delay(500)
        showCup = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1500), // Dorado muy oscuro
                        Color(0xFF0D1B2A),
                        Color(0xFF0A0E1A)
                    )
                )
            )
    ) {
        // Confeti en el fondo
        ConfettiBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Halo de luz y Copa
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Halo palpitante
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonGold.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
                
                // Copa / Escudo Central
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCup,
                    enter = androidx.compose.animation.scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NeonGold, Color(0xFFB8860B))
                                )
                            )
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 70.sp)
                    }
                }
            }

            Text(
                text = "¡FELICIDADES!",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = NeonGold,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AGENTE ${userProgress.agentName.uppercase()}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DungeonCard.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Has superado las ${com.example.cybertura.game.engine.NpcRegistry.questNpcs.size} misiones de los Maestros Cyber, tanto en las calles como dentro de las casas.",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ahora eres un Campeón Legendario de la Ciberseguridad. Estás listo para navegar por el mundo digital con sabiduría y valentía.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Resumen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AchievementStat(icon = "🏅", value = "${com.example.cybertura.game.engine.NpcRegistry.questNpcs.size}/${com.example.cybertura.game.engine.NpcRegistry.questNpcs.size}", label = "Medallas")
                        AchievementStat(icon = "⭐", value = "${userProgress.totalScore}", label = "Puntos")
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onShowCredits,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    "Ver Créditos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = DungeonBlack
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onFinish) {
                Text(
                    "Volver al Mundo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AchievementStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 28.sp)
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMuted,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Genera un fondo animado de confeti cayendo.
 */
@Composable
private fun ConfettiBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiY"
    )

    val confettiList = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                color = listOf(NeonCyan, NeonGold, NeonPink, Color.White).random(),
                size = Random.nextInt(6, 14).toFloat(),
                speed = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        confettiList.forEach { p ->
            val drawY = ((p.yOffset * size.height) + (offsetY * p.speed)) % size.height
            val drawX = p.x * size.width + sin(drawY / 100f) * 20f // Movimiento zig-zag

            drawRect(
                color = p.color,
                topLeft = Offset(drawX, drawY),
                size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.5f)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val yOffset: Float,
    val color: Color,
    val size: Float,
    val speed: Float
)
