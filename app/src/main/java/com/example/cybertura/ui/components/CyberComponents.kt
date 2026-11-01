package com.example.cybertura.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.ui.theme.*
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════
// PULSING GLOW — efecto de aura pulsante tipo MMORPG
// ═══════════════════════════════════════════════════════════════
@Composable
fun PulsingGlowBox(
    glowColor: Color,
    modifier: Modifier = Modifier,
    glowRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha * 0.4f),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx())
                )
            },
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════
// RPG XP BAR — barra de experiencia animada
// ═══════════════════════════════════════════════════════════════
@Composable
fun XpBar(
    currentXp: Int,
    maxXp: Int,
    level: Int,
    modifier: Modifier = Modifier,
    barColor: Color = NeonCyan,
    showLevel: Boolean = true
) {
    val progress by animateFloatAsState(
        targetValue = currentXp.toFloat() / maxXp.toFloat(),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "xpProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "xpShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showLevel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Level badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Brush.linearGradient(listOf(NeonGold, NeonAmber)),
                                CircleShape
                            )
                            .border(1.dp, NeonGold.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$level",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = DungeonBlack
                        )
                    }
                    Text(
                        text = "NIVEL $level",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "$currentXp / $maxXp XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }

        // Actual XP bar with shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DungeonBlack)
                .border(1.dp, DungeonBorder, RoundedCornerShape(5.dp))
        ) {
            // Filled portion with gradient
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.7f), barColor)
                        )
                    )
            )

            // Shimmer overlay
            if (progress > 0.01f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                ) {
                    val shimmerWidth = size.width * 0.3f
                    val shimmerX = size.width * shimmerOffset
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startX = shimmerX - shimmerWidth,
                            endX = shimmerX + shimmerWidth
                        )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RPG TOP BAR — HUD de Personaje estilo MMORPG
// ═══════════════════════════════════════════════════════════════
@Composable
fun CyberTopBar(
    agentName: String,
    points: Int,
    onBadgesClick: () -> Unit,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    progress: UserProgress? = null
) {
    val level = progress?.level ?: ((points / 200) + 1)
    val rankTitle = progress?.rankTitle ?: "Novato Digital"

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
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showBack) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(DungeonCardHover, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Avatar con nivel
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan, NeonPurple)
                                        )
                                    )
                                    .border(2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Avatar",
                                    tint = DungeonBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Level badge sobre el avatar
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(18.dp)
                                    .background(
                                        Brush.linearGradient(listOf(NeonGold, NeonAmber)),
                                        CircleShape
                                    )
                                    .border(1.dp, DungeonBlack, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$level",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DungeonBlack
                                )
                            }
                        }

                        Column {
                            Text(
                                text = agentName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = rankTitle,
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Trofeos
                        Surface(
                            onClick = onBadgesClick,
                            shape = RoundedCornerShape(10.dp),
                            color = DungeonCardHover,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, NeonGold.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trofeos",
                                    tint = NeonGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // XP total (es la misma cifra que define el nivel; no hay otra moneda)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DungeonCardHover,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, NeonGold.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = NeonGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$points",
                                    color = NeonGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "XP",
                                    color = NeonAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Mini XP bar en el HUD
                if (progress != null) {
                    XpBar(
                        currentXp = progress.currentXp,
                        maxXp = progress.xpToNextLevel,
                        level = progress.level,
                        showLevel = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MODULE ICON — ícono con aura de rareza RPG
// ═══════════════════════════════════════════════════════════════
@Composable
fun CyberModuleIcon(
    iconName: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false
) {
    val icon: ImageVector = when (iconName) {
        "vpn_key" -> Icons.Default.VpnKey
        "phishing" -> Icons.Default.Search
        "shield" -> Icons.Default.Security
        "favorite" -> Icons.Default.Favorite
        "public" -> Icons.Default.Public
        "wifi" -> Icons.Default.Home
        "sports_esports" -> Icons.Default.Star
        "bug_report" -> Icons.Default.BugReport
        else -> Icons.Default.Security
    }

    val infiniteTransition = rememberInfiniteTransition(label = "iconGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconGlowAlpha"
    )

    val borderColor = if (isCompleted) NeonGold else accentColor

    Box(
        modifier = modifier
            .drawBehind {
                // Outer glow aura
                drawRoundRect(
                    color = borderColor.copy(alpha = glowAlpha * 0.25f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        accentColor.copy(alpha = 0.25f),
                        DungeonBlack.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = if (isCompleted) 2.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor.copy(alpha = glowAlpha),
                        borderColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(30.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FEEDBACK MODAL — Pantalla de resultado estilo RPG
// ═══════════════════════════════════════════════════════════════
@Composable
fun FeedbackModal(
    isCorrect: Boolean,
    title: String,
    feedbackText: String,
    educationalTip: String,
    pointsAwarded: Int = 100,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isCorrect) QuestSuccess else QuestRetry
    val icon = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Lightbulb

    val infiniteTransition = rememberInfiniteTransition(label = "feedbackGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "modalGlow"
    )

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isCorrect) "⚔️ ¡Continuar Misión!" else "💡 Entendido, ¡seguir!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = DungeonBlack
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // RPG result icon with glow
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .drawBehind {
                            drawCircle(
                                color = accent.copy(alpha = glowAlpha * 0.3f),
                                radius = size.minDimension * 0.8f
                            )
                        }
                        .background(accent.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, accent.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isCorrect) "¡VICTORIA!" else "¡SIGUE INTENTANDO!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                // XP awarded badge
                if (isCorrect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = NeonGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, NeonGold.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "✦", color = NeonGold, fontSize = 14.sp)
                            Text(
                                text = "+$pointsAwarded XP",
                                color = NeonGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = feedbackText,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                if (educationalTip.isNotBlank()) {
                    Surface(
                        color = DungeonBlack.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, ShieldGlow.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(ShieldGlow.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Skill",
                                    tint = ShieldGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "🛡️ CONOCIMIENTO DESBLOQUEADO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ShieldGlow,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = educationalTip,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DungeonCard,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════
// FLOATING PARTICLES — partículas decorativas (fondo)
// ═══════════════════════════════════════════════════════════════
@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleColor: Color = NeonCyan.copy(alpha = 0.15f),
    count: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val offsets = remember {
        List(count) {
            Pair(
                (Math.random() * 1.0).toFloat(),
                (Math.random() * 1.0).toFloat()
            )
        }
    }

    val animatedY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleY"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        offsets.forEachIndexed { i, (x, baseY) ->
            val speed = 0.3f + (i % 4) * 0.2f
            val yPos = ((baseY + animatedY * speed) % 1.1f) * size.height
            val xWobble = sin((animatedY + i) * 3.14f * 2) * 15f
            val radius = 1.5f + (i % 3) * 1.2f
            val alpha = 0.2f + (sin((animatedY + i * 0.5f) * 3.14f).toFloat() * 0.3f)

            drawCircle(
                color = particleColor.copy(alpha = alpha.coerceIn(0.05f, 0.5f)),
                radius = radius.dp.toPx(),
                center = Offset(
                    x = x * size.width + xWobble.dp.toPx(),
                    y = yPos
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RARITY BORDER — borde animado según rareza del módulo
// ═══════════════════════════════════════════════════════════════
@Composable
fun RarityBorderModifier(
    isCompleted: Boolean,
    accentColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp)
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "rarity")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )

    return if (isCompleted) {
        Modifier.border(
            width = 2.dp,
            brush = Brush.linearGradient(
                listOf(
                    NeonGold.copy(alpha = borderAlpha),
                    NeonAmber.copy(alpha = borderAlpha * 0.6f),
                    NeonGold.copy(alpha = borderAlpha)
                )
            ),
            shape = shape
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.25f),
            shape = shape
        )
    }
}
