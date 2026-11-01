package com.example.cybertura.ui.screens

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
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.game.engine.GameState
import com.example.cybertura.game.engine.GameWorld
import com.example.cybertura.game.engine.MapIds
import com.example.cybertura.game.engine.NpcData
import com.example.cybertura.game.engine.NpcRegistry
import com.example.cybertura.game.renderer.drawSprites
import com.example.cybertura.game.renderer.drawWorldMap
import com.example.cybertura.ui.components.CyberTopBar
import com.example.cybertura.ui.theme.*
import com.example.cybertura.ui.viewmodel.GameWorldViewModel
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
    val gameState by viewModel.gameState.collectAsState()
    val context = LocalContext.current

    // --- Assets de Ambiente (remember evita recargas en cada recomposición) ---
    val worldAssets = remember {
        com.example.cybertura.game.renderer.WorldAssets(
            grassSheet    = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_earth),
            waterImg      = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_water),
            water5Img     = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.water5_pipo),
            earthImg      = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_earth),
            baseImg       = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_base),
            cornerImg     = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_corner_ld),
            crossroadsImg = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_crossroads),
            lairImg       = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_lair),
            bonesImg      = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_bones),
            bushImg       = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_bush),
            cartelImg     = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_cartel),
            graveImg      = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_grave),
            lukeImg       = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_luke),
            mushroomImg   = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_mushroom),
            skullImg      = androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.tile_skull)
        )
    }


    // --- Assets de Personajes ---
    val playerSprite = remember {
        androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.thief_idle_1)
    }
    val npcSprites = remember {
        mapOf(
            "npc_passwords"  to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.alchemist_idle_1),
            "npc_phishing"   to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.blacksmith_idle_1),
            "npc_privacy"    to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.princess_idle_1),
            "npc_bullying"   to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.mage_idle_1),
            "npc_social"     to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.king_idle_1),
            "npc_villager_1" to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.townsfolk_m_idle_1),
            "npc_villager_2" to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.butcher_idle_1),
            "npc_villager_3" to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.herald_idle_1),
            // Maestros y habitantes dentro de las casas
            "npc_wifi"       to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.herald_idle_1),
            "npc_gaming"     to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.executioner_idle_1),
            "npc_forest"     to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.alchemist_idle_1),
            "npc_abuela"     to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.princess_idle_1),
            "npc_tio_bits"   to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.butcher_idle_1),
            "npc_mama_cora"  to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.townsfolk_m_idle_1),
            "npc_hermana_luna" to androidx.compose.ui.graphics.ImageBitmap.imageResource(context.resources, com.example.cybertura.R.drawable.mage_idle_1)
        )
    }

    // --- Bucle de juego: corre solo mientras esta pantalla está visible ---
    LifecycleStartEffect(viewModel) {
        viewModel.resumeGameLoop()
        onStopOrDispose { viewModel.pauseGameLoop() }
    }

    // --- Sonido ---
    // Pasos en bucle mientras el jugador camina; se cortan al salir de la pantalla
    LaunchedEffect(gameState.isWalking) {
        SoundManager.setWalking(gameState.isWalking)
    }
    DisposableEffect(Unit) {
        onDispose { SoundManager.setWalking(false) }
    }
    // Al abrir un diálogo con un NPC
    LaunchedEffect(gameState.showDialog) {
        if (gameState.showDialog) SoundManager.playChat()
    }
    // Al cruzar una puerta, tanto al entrar como al salir de una casa (no al cargar la pantalla)
    var previousMapId by remember { mutableStateOf(gameState.currentMapId) }
    LaunchedEffect(gameState.currentMapId) {
        if (gameState.currentMapId != previousMapId) {
            SoundManager.playDoor()
        }
        previousMapId = gameState.currentMapId
    }

    // Desvanecimiento al cruzar una puerta + cartel con el nombre del lugar
    val fade = remember { Animatable(0f) }
    var showPlaceChip by remember { mutableStateOf(false) }
    LaunchedEffect(gameState.currentMapId) {
        fade.snapTo(1f)
        fade.animateTo(0f, tween(450))
        if (GameWorld.isInterior(gameState.currentMapId)) {
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
            // La cámara sigue al jugador
            val cameraX = gameState.playerX + 0.5f
            val cameraY = gameState.playerY + 0.5f
            val isInside = GameWorld.isInterior(gameState.currentMapId)

            // Dibujar el mapa (calle o interior) con todos los assets
            drawWorldMap(cameraX, cameraY, worldAssets, gameState.currentMapId)

            // Dibujar NPCs y Jugador con sus nuevas imágenes
            drawSprites(gameState, NpcRegistry.npcs, playerSprite, npcSprites)

            // Ambiente: oscuro en la calle (atmósfera RPG), cálido y claro dentro de casa,
            // y en penumbra bajo los árboles del bosque
            val shade = when {
                gameState.currentMapId == MapIds.FOREST -> 0.22f
                isInside -> 0.1f
                else -> 0.45f
            }
            drawRect(color = Color.Black.copy(alpha = shade))

            // Partículas (Lluvia Digital)
            for (p in gameState.particles) {
                drawRect(
                    color = NeonCyan.copy(alpha = p.alpha),
                    topLeft = Offset(p.x * size.width, p.y * size.height),
                    size = androidx.compose.ui.geometry.Size(2f, p.size * 3f)
                )
            }
        }

        // 2. HUD Superior (Monedas, nivel, etc) + objetivo actual
        Column(modifier = Modifier.fillMaxWidth()) {
            CyberTopBar(
                agentName = userProgress.agentName,
                points = userProgress.totalScore,
                onBadgesClick = onBadgesClick,
                progress = userProgress,
                showBack = false
            )
            ObjectiveBanner(gameState)
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
                        text = GameWorld.mapName(gameState.currentMapId),
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
                    isHighlighted = gameState.nearbyNpcId != null && !gameState.showDialog,
                    onClick = { viewModel.interact() }
                )
            }
        }

        // 4. Sistema de Diálogo tipo Pokémon
        AnimatedVisibility(
            visible = gameState.showDialog,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            gameState.dialogNpcId?.let { npcId ->
                val npc = NpcRegistry.getNpcById(npcId)
                if (npc != null) {
                    PokemonDialogBox(
                        npc = npc,
                        dialogText = gameState.dialogText,
                        isBlocked = gameState.dialogIsBlocked,
                        onAcceptQuest = {
                            viewModel.closeDialog()
                            // Si es un NPC de misión, navegar. Si no, solo se cerrará el diálogo
                            if (npc.moduleId != null && !gameState.dialogIsBlocked) {
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
private fun ObjectiveBanner(state: GameState) {
    val target = state.nextQuestNpcId?.let { NpcRegistry.getNpcById(it) }
    val inside = GameWorld.isInterior(state.currentMapId)

    val title: String
    val detail: String
    when {
        target == null -> {
            title = "🏆 ¡Todas las misiones completadas!"
            detail = "Eres un Campeón Legendario. Explora CyberTura cuando quieras."
        }
        target.mapId == state.currentMapId -> {
            title = "🎯 Habla con ${target.name}"
            detail = if (inside) "¡Está aquí dentro! Acércate y presiona A." else "Está ${target.locationHint}."
        }
        inside -> {
            title = if (state.currentMapId == MapIds.FOREST) "🌲 Este no es el lugar de tu misión" else "🚪 Esta no es la casa de tu misión"
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

@Composable
fun PokemonDialogBox(
    npc: NpcData,
    dialogText: String,
    isBlocked: Boolean,
    onAcceptQuest: () -> Unit,
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
                        color = TextMuted
                    )
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
                        if (npc.moduleId != null && !isBlocked) {
                            TextButton(onClick = onCancel) {
                                Text("Más tarde", color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onAcceptQuest,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("¡Aceptar Reto!", color = DungeonBlack, fontWeight = FontWeight.Black)
                            }
                        } else {
                            // Es un aldeano o está bloqueado
                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Entendido", color = DungeonBlack, fontWeight = FontWeight.Black)
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
