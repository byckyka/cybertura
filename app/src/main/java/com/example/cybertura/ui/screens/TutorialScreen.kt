package com.example.cybertura.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.game.engine.NpcRegistry
import com.example.cybertura.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Pantalla de tutorial que se muestra la primera vez que el usuario abre el juego.
 * Presenta a los 5 maestros de misión en orden y explica el flujo del juego.
 */
@Composable
fun TutorialScreen(
    onTutorialFinished: () -> Unit
) {
    val questNpcs = NpcRegistry.questNpcs  // Los 5 maestros en orden
    var currentSlide by remember { mutableIntStateOf(0) }

    // Slide 0 = bienvenida, slides 1-5 = NPCs, slide 6 = ¡A jugar!
    val totalSlides = questNpcs.size + 2 // bienvenida + un slide por maestro + cierre

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E1A),
                        Color(0xFF0D1B2A),
                        Color(0xFF0A0E1A)
                    )
                )
            )
    ) {
        // Partículas decorativas de fondo
        TutorialParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Indicador de progreso de slides
            SlideProgressIndicator(
                current = currentSlide,
                total = totalSlides,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Contenido central animado
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(400),
                        initialOffsetX = { if (targetState > initialState) it else -it }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(400),
                        targetOffsetX = { if (targetState > initialState) -it else it }
                    )
                },
                label = "tutorialSlide",
                modifier = Modifier.weight(1f)
            ) { slide ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (slide) {
                        0 -> WelcomeSlide()
                        totalSlides - 1 -> ReadySlide()
                        else -> {
                            val npc = questNpcs.getOrNull(slide - 1)
                            if (npc != null) NpcSlide(
                                emoji = npc.emoji,
                                name = npc.name,
                                title = npc.title,
                                order = slide,
                                description = npc.tutorialDialog
                            )
                        }
                    }
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Anterior (oculto en primer slide)
                if (currentSlide > 0) {
                    OutlinedButton(
                        onClick = { currentSlide-- },
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Text("← Anterior", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(100.dp))
                }

                // Botón Saltar (solo en slides de NPC)
                if (currentSlide in 1 until totalSlides - 1) {
                    TextButton(
                        onClick = onTutorialFinished,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
                    ) {
                        Text("Saltar", fontSize = 13.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                // Botón Siguiente / ¡Empezar!
                val isLast = currentSlide == totalSlides - 1
                Button(
                    onClick = {
                        if (isLast) {
                            onTutorialFinished()
                        } else {
                            currentSlide++
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLast) NeonGold else NeonCyan.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = if (isLast) "¡A Jugar! 🚀" else "Siguiente →",
                        fontWeight = FontWeight.Black,
                        color = DungeonBlack,
                        fontSize = if (isLast) 16.sp else 14.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide de Bienvenida
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeSlide() {
    val infiniteTransition = rememberInfiniteTransition(label = "welcomePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        // Logo animado
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
                .border(3.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡️", fontSize = 56.sp)
        }

        Text(
            text = "¡Bienvenido a\nCyberTura!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Text(
                text = "En este mundo viven ${NpcRegistry.questNpcs.size} Maestros Cyber que te enseñarán habilidades esenciales para ser un héroe digital. ¡Algunos te esperan dentro de sus casas!\n\nSigue el signo ❗ dorado: te guiará por la calle y hasta la puerta correcta. ¡Completa sus retos en orden para convertirte en Campeón Legendario!",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Instrucción de controles
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlHint(icon = "🕹️", label = "Muévete con\nel joystick")
            ControlHint(icon = "🅰", label = "Presiona A para\nhablar con NPCs")
        }
    }
}

@Composable
private fun ControlHint(icon: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeonCyan.copy(alpha = 0.15f))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide de cada NPC
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NpcSlide(
    emoji: String,
    name: String,
    title: String,
    order: Int,
    description: String
) {
    // Animación de tipeo
    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(description) {
        displayedText = ""
        description.forEach { char ->
            displayedText += char
            delay(12)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        // Badge de orden
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NeonGold.copy(alpha = 0.2f))
                .border(1.dp, NeonGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = "MISIÓN #$order de ${NpcRegistry.questNpcs.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = NeonGold,
                letterSpacing = 1.sp
            )
        }

        // Avatar del NPC
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.2f),
                            Color(0xFF0D1B2A)
                        )
                    )
                )
                .border(3.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 44.sp)
        }

        // Nombre y título
        Text(
            text = name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            textAlign = TextAlign.Center
        )
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        // Cuadro de diálogo estilo RPG
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(2.dp, DungeonBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Triángulo indicador (simulado con texto)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(NeonCyan, CircleShape)
                    )
                    Text(
                        text = name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayedText,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide de cierre
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReadySlide() {
    val infiniteTransition = rememberInfiniteTransition(label = "readyGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("🏆", fontSize = 80.sp)

        Text(
            text = "¡Todo listo,\nAventurero!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = NeonGold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(2.dp, NeonGold.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recuerda el orden de tu misión:",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                NpcRegistry.questNpcs.forEach { npc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${npc.questOrder}.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGold,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(npc.emoji, fontSize = 18.sp)
                        Column {
                            Text(
                                text = npc.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = npc.title,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Busca el marcador ❗ sobre los personajes\npara saber a quién visitar primero.",
            fontSize = 13.sp,
            color = TextSecondary.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Indicador de progreso de slides
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SlideProgressIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (isActive) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) NeonCyan
                        else if (index < current) NeonCyan.copy(alpha = 0.4f)
                        else Color.White.copy(alpha = 0.15f)
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Partículas decorativas de fondo
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TutorialParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleOffset"
    )

    // Estrellas estáticas decorativas
    Box(modifier = Modifier.fillMaxSize()) {
        listOf(
            Pair(0.1f, 0.05f), Pair(0.85f, 0.1f), Pair(0.3f, 0.02f),
            Pair(0.7f, 0.08f), Pair(0.05f, 0.15f), Pair(0.92f, 0.2f),
            Pair(0.15f, 0.25f), Pair(0.78f, 0.18f), Pair(0.5f, 0.03f)
        ).forEachIndexed { i, (x, y) ->
            val animY = ((y + offset * 0.05f + i * 0.07f) % 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        x = (x * 400).dp,
                        y = (animY * 800).dp
                    )
                    .size(2.dp)
                    .background(NeonCyan.copy(alpha = 0.3f + (i % 3) * 0.1f), CircleShape)
            )
        }
    }
}
