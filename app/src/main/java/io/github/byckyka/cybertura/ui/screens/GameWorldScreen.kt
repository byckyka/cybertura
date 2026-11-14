package io.github.byckyka.cybertura.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import io.github.byckyka.cybertura.audio.SoundManager
import io.github.byckyka.cybertura.data.model.UserProgress
import io.github.byckyka.cybertura.game.engine.DialogAction
import io.github.byckyka.cybertura.game.engine.GameState
import io.github.byckyka.cybertura.game.engine.GameWorld
import io.github.byckyka.cybertura.game.engine.MapIds
import io.github.byckyka.cybertura.game.engine.NpcData
import io.github.byckyka.cybertura.game.engine.NpcRegistry
import io.github.byckyka.cybertura.game.renderer.drawSprites
import io.github.byckyka.cybertura.ui.components.NarrateButton
import io.github.byckyka.cybertura.game.renderer.drawWorldMap
import io.github.byckyka.cybertura.ui.components.CyberTopBar
import io.github.byckyka.cybertura.ui.theme.*
import io.github.byckyka.cybertura.ui.viewmodel.GameWorldViewModel
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameWorldScreen(
    viewModel: GameWorldViewModel,
    userProgress: UserProgress,
    onNavigateToModule: (String) -> Unit,
    onBadgesClick: () -> Unit
) {
    // El estado completo cambia en cada cuadro: solo se lee dentro del Canvas (fase de dibujo).
    val frameState = viewModel.gameState.collectAsState()
    // Lo que usa el resto de la pantalla cambia pocas veces; al observarlo aparte, los botones,
    // el banner y el diálogo no se recomponen 60 veces por segundo.
    val ui by remember {
        derivedStateOf {
            frameState.value.let {
                WorldUiState(
                    isWalking = it.isWalking,
                    showDialog = it.showDialog,
                    currentMapId = it.currentMapId,
                    nearbyNpcId = it.nearbyNpcId,
                    dialogNpcId = it.dialogNpcId,
                    dialogText = it.dialogText,
                    dialogAction = it.dialogAction,
                    nextQuestNpcId = it.nextQuestNpcId
                )
            }
        }
    }
    val context = LocalContext.current

    // --- Imágenes: se cargan una sola vez, no al volver de cada reto ---
    val sprites = remember { io.github.byckyka.cybertura.game.renderer.GameSprites.get(context.resources) }
    val worldAssets = sprites.world
    val playerSprite = sprites.playerFor(userProgress.agentCharacter)
    val npcSprites = sprites.npcs

    // --- Bucle de juego: corre solo mientras esta pantalla está visible ---
    LifecycleStartEffect(viewModel) {
        viewModel.resumeGameLoop()
        onStopOrDispose { viewModel.pauseGameLoop() }
    }

    // --- Sonido ---
    // Pasos en bucle mientras el jugador camina; se cortan al salir de la pantalla
    LaunchedEffect(ui.isWalking) {
        SoundManager.setWalking(ui.isWalking)
    }
    DisposableEffect(Unit) {
        onDispose { SoundManager.setWalking(false) }
    }
    // Al abrir un diálogo con un NPC
    LaunchedEffect(ui.showDialog) {
        if (ui.showDialog) SoundManager.playChat()
    }
    // Al cruzar una puerta, tanto al entrar como al salir de una casa (no al cargar la pantalla)
    var previousMapId by remember { mutableStateOf(ui.currentMapId) }
    LaunchedEffect(ui.currentMapId) {
        if (ui.currentMapId != previousMapId) {
            SoundManager.playDoor()
        }
        previousMapId = ui.currentMapId
    }

    // Desvanecimiento al cruzar una puerta + cartel con el nombre del lugar
    val fade = remember { Animatable(0f) }
    var showPlaceChip by remember { mutableStateOf(false) }
    LaunchedEffect(ui.currentMapId) {
        fade.snapTo(1f)
        fade.animateTo(0f, tween(450))
        if (GameWorld.isInterior(ui.currentMapId)) {
            showPlaceChip = true
            delay(2600)
        }
        showPlaceChip = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. El Mundo 2D (Renderizado en Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = frameState.value
            // La cámara sigue al jugador
            val cameraX = frame.playerX + 0.5f
            val cameraY = frame.playerY + 0.5f
            val isInside = GameWorld.isInterior(ui.currentMapId)

            // Dibujar el mapa (calle o interior) con todos los assets
            drawWorldMap(cameraX, cameraY, worldAssets, ui.currentMapId)

            // Dibujar NPCs y Jugador con sus nuevas imágenes
            drawSprites(frame, NpcRegistry.npcs, playerSprite, npcSprites)

            // Ambiente: oscuro en la calle (atmósfera RPG), cálido y claro dentro de casa,
            // y en penumbra bajo los árboles del bosque
            val shade = when {
                ui.currentMapId == MapIds.FOREST -> 0.22f
                isInside -> 0.1f
                else -> 0.45f
            }
            drawRect(color = Color.Black.copy(alpha = shade))

            // Partículas (Lluvia Digital)
            for (p in frame.particles) {
                drawRect(
                    color = NeonCyan.copy(alpha = p.alpha),
                    topLeft = Offset(p.x * size.width, p.y * size.height),
                    size = androidx.compose.ui.geometry.Size(2f, p.size * 3f)
                )
            }
        }

        // 2. HUD Superior (XP, nivel, etc) + objetivo actual
        Column(modifier = Modifier.fillMaxWidth()) {
            CyberTopBar(
                agentName = userProgress.agentName,
                points = userProgress.totalScore,
                onBadgesClick = onBadgesClick,
                progress = userProgress,
                showBack = false
            )
            ObjectiveBanner(ui.nextQuestNpcId, ui.currentMapId)
            AnimatedVisibility(
                visible = showPlaceChip,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DungeonCard.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = GameWorld.mapName(ui.currentMapId),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Fundido a negro al cruzar una puerta
        if (fade.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fade.value))
            )
        }

        // 3. Controles - Joystick y Botón A
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Joystick
                VirtualJoystick(
                    onJoystickMove = { x, y -> viewModel.updateJoystick(x, y) },
                    onJoystickRelease = { viewModel.updateJoystick(0f, 0f) }
                )

                // Botón Acción A
                ActionButton(
                    isHighlighted = ui.nearbyNpcId != null && !ui.showDialog,
                    onClick = { viewModel.interact() }
                )
            }
        }

        // 4. Sistema de Diálogo tipo Pokémon
        AnimatedVisibility(
            visible = ui.showDialog,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ui.dialogNpcId?.let { npcId ->
                val npc = NpcRegistry.getNpcById(npcId)
                if (npc != null) {
                    PokemonDialogBox(
                        npc = npc,
                        dialogText = ui.dialogText,
                        action = ui.dialogAction,
                        onStartQuest = {
                            viewModel.closeDialog()
                            // Aceptar el reto o repasarlo llevan a la misma pantalla del módulo
                            if (npc.moduleId != null && ui.dialogAction != DialogAction.NONE) {
                                onNavigateToModule(npc.moduleId)
                            }
                        },
                        onCancel = { viewModel.closeDialog() }
                    )
                }
            }
        }
    }
}

// --- Controles de UI ---

/** Franja con el objetivo actual: a quién buscar y, si está dentro de una casa, cuándo entrar. */
@Composable
private fun ObjectiveBanner(nextQuestNpcId: String?, currentMapId: String) {
    val target = nextQuestNpcId?.let { NpcRegistry.getNpcById(it) }
    val inside = GameWorld.isInterior(currentMapId)

    val title: String
    val detail: String
    when {
        target == null -> {
            title = "🏆 ¡Todas las misiones completadas!"
            detail = "Eres un Campeón Legendario. Explora CyberTura cuando quieras."
        }
        target.mapId == currentMapId -> {
            title = "🎯 Habla con ${target.name}"
            detail = if (inside) "¡Está aquí dentro! Acércate y presiona A." else "Está ${target.locationHint}."
        }
        inside -> {
            title = if (currentMapId == MapIds.FOREST) "🌲 Este no es el lugar de tu misión" else "🚪 Esta no es la casa de tu misión"
            detail = "Sal por la flecha verde de abajo. ${target.name} está ${target.locationHint}."
        }
        target.mapId == MapIds.FOREST -> {
            title = "🌲 ¡Es hora de entrar al bosque!"
            detail = "${target.name} está ${target.locationHint}. Camina hasta la puerta morada con el ❗ dorado."
        }
        target.mapId != MapIds.EXTERIOR -> {
            title = "🏠 ¡Es hora de entrar a una casa!"
            detail = "${target.name} está ${target.locationHint}. Camina hasta la puerta con el ❗ dorado."
        }
        else -> {
            title = "🎯 Siguiente maestro: ${target.name}"
            detail = "Lo encuentras ${target.locationHint}. Sigue la flecha dorada."
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DungeonCard.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGold.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = NeonGold
            )
            Text(
                text = detail,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun VirtualJoystick(
    onJoystickMove: (Float, Float) -> Unit,
    onJoystickRelease: () -> Unit
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 120f // pixels
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        stickOffset = Offset.Zero
                        onJoystickRelease()
                    },
                    onDragCancel = {
                        stickOffset = Offset.Zero
                        onJoystickRelease()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = stickOffset + dragAmount
                        
                        // Restringir el movimiento al radio del círculo exterior
                        val distance = newOffset.getDistance()
                        if (distance > maxRadius) {
                            val angle = atan2(newOffset.y, newOffset.x)
                            stickOffset = Offset(cos(angle) * maxRadius, sin(angle) * maxRadius)
                        } else {
                            stickOffset = newOffset
                        }

                        // Normalizar de -1.0 a 1.0 para el engine
                        onJoystickMove(stickOffset.x / maxRadius, stickOffset.y / maxRadius)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Círculo central (el que se mueve)
        Box(
            modifier = Modifier
                .offset(x = (stickOffset.x / density).dp, y = (stickOffset.y / density).dp)
                .size(50.dp)
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
        )
    }
}

@Composable
fun ActionButton(
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btnHighlight")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isHighlighted) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )

    val color = if (isHighlighted) NeonGold else Color.White.copy(alpha = 0.5f)

    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size((70 * scale).dp)
            .border(2.dp, color, CircleShape)
    ) {
        Text(
            text = "A",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.8f)
        )
    }
}

/** Lo que la pantalla del mundo necesita fuera del Canvas; cambia solo con acciones del jugador. */
private data class WorldUiState(
    val isWalking: Boolean,
    val showDialog: Boolean,
    val currentMapId: String,
    val nearbyNpcId: String?,
    val dialogNpcId: String?,
    val dialogText: String,
    val dialogAction: DialogAction,
    val nextQuestNpcId: String?
)

@Composable
fun PokemonDialogBox(
    npc: NpcData,
    dialogText: String,
    action: DialogAction,
    onStartQuest: () -> Unit,
    onCancel: () -> Unit
) {
    // Animación de máquina de escribir
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(true) }

    LaunchedEffect(dialogText) {
        displayedText = ""
        isTyping = true
        for (i in dialogText.indices) {
            displayedText += dialogText[i]
            delay(20) // Velocidad de tipeo
        }
        isTyping = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 80.dp) // Espacio sobre los controles
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DungeonCard,
            border = androidx.compose.foundation.BorderStroke(3.dp, DungeonBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header (Nombre del NPC)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = npc.name.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "[${npc.title}]",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    NarrateButton(text = dialogText)
                }

                // Texto del diálogo (máquina de escribir)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = displayedText,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        lineHeight = 24.sp,
                        overflow = TextOverflow.Visible
                    )
                }

                // Botones de acción (solo si terminó de tipear)
                if (!isTyping) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (action) {
                            DialogAction.ACCEPT_QUEST -> {
                                TextButton(onClick = onCancel) {
                                    Text("Más tarde", color = TextSecondary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onStartQuest,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("¡Aceptar Reto!", color = DungeonBlack, fontWeight = FontWeight.Black)
                                }
                            }
                            DialogAction.REPLAY_QUEST -> {
                                // Maestro ya superado: se puede repasar su reto las veces que se quiera
                                TextButton(onClick = onStartQuest) {
                                    Text("🔁 Repasar reto", color = NeonGold, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onCancel,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Entendido", color = DungeonBlack, fontWeight = FontWeight.Black)
                                }
                            }
                            DialogAction.NONE -> {
                                // Es un aldeano o un maestro que aún no toca
                                Button(
                                    onClick = onCancel,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Entendido", color = DungeonBlack, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                } else {
                    // Indicador de "saltar diálogo" (tocar en la caja lo llenaría, pero simplificamos)
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
                        Text("...", color = TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
