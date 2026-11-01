package com.example.cybertura.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.data.repository.CyberRepository
import com.example.cybertura.game.engine.Direction
import com.example.cybertura.game.engine.GameState
import com.example.cybertura.game.engine.GameWorld
import com.example.cybertura.game.engine.MapIds
import com.example.cybertura.game.engine.NpcRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

class GameWorldViewModel(
    private val repository: CyberRepository
) : ViewModel() {

    // Inicializar estado de NPCs dinámico
    private val initialNpcStates = NpcRegistry.npcs.associate { npc ->
        npc.id to com.example.cybertura.game.engine.NpcState(
            id = npc.id,
            x = npc.tileX,
            y = npc.tileY,
            startX = npc.tileX,
            startY = npc.tileY,
            targetX = npc.tileX,
            targetY = npc.tileY,
            waitTimerMs = (1000..3000).random().toLong()
        )
    }

    // Mapa y radio de paseo de cada NPC (constantes)
    private val npcMaps = NpcRegistry.npcs.associate { it.id to it.mapId }
    private val npcWander = NpcRegistry.npcs.associate { it.id to it.wanderRadius }

    private val _gameState = MutableStateFlow(
        GameState(
            playerX = GameWorld.playerSpawnX,
            playerY = GameWorld.playerSpawnY,
            npcStates = initialNpcStates,
            nextQuestNpcId = NpcRegistry.getNextQuestNpcId(emptySet())
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    /** Bucle de juego activo; null mientras está en pausa. */
    private var gameLoopJob: Job? = null

    /** Módulos completados por el jugador (para saber qué maestros ya superó). */
    private var completedModuleIds: Set<String> = emptySet()

    init {
        // El bucle no arranca aquí: la pantalla lo reanuda y lo pausa según su ciclo de vida.
        observeProgressForQuestMarker()
    }

    /** Observa el progreso del usuario y actualiza el marcador de misión activa */
    private fun observeProgressForQuestMarker() {
        viewModelScope.launch {
            repository.userProgress.collect { progress ->
                completedModuleIds = progress.completedModuleIds
                val nextNpcId = NpcRegistry.getNextQuestNpcId(progress.completedModuleIds)
                _gameState.update { it.copy(nextQuestNpcId = nextNpcId) }
            }
        }
    }

    /**
     * Arranca el bucle de juego. Se llama cuando la pantalla del mundo es visible
     * (ON_START); no hace nada si ya está corriendo.
     */
    fun resumeGameLoop() {
        if (gameLoopJob?.isActive == true) return

        gameLoopJob = viewModelScope.launch {
            // Reiniciar el reloj para que el primer frame no cuente el tiempo en pausa
            _gameState.update { it.copy(lastFrameTimeMs = System.currentTimeMillis()) }

            while (isActive) {
                val frameStart = System.currentTimeMillis()
                val currentState = _gameState.value
                // dt limitado a 100ms para evitar saltos grandes en frames lentos
                val dt = ((frameStart - currentState.lastFrameTimeMs) / 1000f).coerceAtMost(0.1f)

                updateGame(dt, frameStart)

                // Delay adaptativo: descuenta el tiempo que tardó el frame
                val elapsed = System.currentTimeMillis() - frameStart
                val remaining = (16L - elapsed).coerceAtLeast(1L)
                delay(remaining)
            }
        }
    }

    /**
     * Detiene el bucle de juego cuando la pantalla deja de verse (ON_STOP): al abrir
     * un reto, las medallas o al mandar la app a segundo plano. Así no gasta batería.
     */
    fun pauseGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        // Soltar el joystick para que el jugador no siga caminando al volver
        _gameState.update { it.copy(joystickX = 0f, joystickY = 0f, isWalking = false) }
    }

    private fun updateGame(dt: Float, currentTime: Long) {
        _gameState.update { state ->

            // 1. Actualizar movimiento basado en el joystick
            var newX = state.playerX
            var newY = state.playerY
            var direction = state.playerDirection
            var isWalking = false

            // Solo mover si NO estamos en un diálogo
            if (!state.showDialog) {
                // Joystick muerto (zona central inactiva para evitar micro-movimientos)
                val magnitude = sqrt(state.joystickX * state.joystickX + state.joystickY * state.joystickY)

                if (magnitude > 0.1f) {
                    isWalking = true

                    val moveDist = GameState.PLAYER_SPEED * dt
                    val dx = state.joystickX * moveDist
                    val dy = state.joystickY * moveDist

                    // Determinar dirección basada en input mayoritario
                    if (abs(state.joystickX) > abs(state.joystickY)) {
                        direction = if (state.joystickX > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        direction = if (state.joystickY > 0) Direction.DOWN else Direction.UP
                    }

                    // 2. Resolver colisiones eje por eje (para resbalar en paredes).
                    // El eje Y se valida con la X YA resuelta: si no, en diagonal se colaba
                    // por la esquina de los obstáculos y el jugador quedaba incrustado.
                    val map = state.currentMapId
                    val movedX = moveAxis(map, newX, newY, dx, horizontal = true)
                    val movedY = moveAxis(map, movedX, newY, dy, horizontal = false)

                    // Asistencia de esquinas: si un eje quedó bloqueado, acomodar suavemente
                    // al jugador hacia el centro del hueco (puertas, pasillos de 1 tile).
                    newX = movedX
                    newY = movedY
                    if (abs(dy) > abs(dx) && abs(movedY - state.playerY) < abs(dy) * 0.5f) {
                        newX = cornerAssist(map, newX, newY, dy, moveDist, horizontal = false)
                    } else if (abs(dx) > abs(dy) && abs(movedX - state.playerX) < abs(dx) * 0.5f) {
                        newY = cornerAssist(map, newX, newY, dx, moveDist, horizontal = true)
                    }
                }
            }

            // 2b. Puertas: pisar la puerta de una casa (o su salida) cambia de mapa
            var currentMap = state.currentMapId
            var joyX = state.joystickX
            var joyY = state.joystickY
            if (!state.showDialog) {
                val portal = GameWorld.portalAt(currentMap, newX, newY)
                if (portal != null) {
                    currentMap = portal.toMap
                    newX = portal.toX
                    newY = portal.toY
                    direction = if (GameWorld.isInterior(portal.toMap)) Direction.UP else Direction.DOWN
                    isWalking = false
                    // Soltar el joystick para no atravesar la puerta de regreso al instante
                    joyX = 0f
                    joyY = 0f
                }
            }

            // 3. Detectar NPC cercano del mapa actual (el más cercano, distancia cuadrada)
            val interactRadiusSq = GameState.NPC_INTERACT_RADIUS * GameState.NPC_INTERACT_RADIUS
            var nearbyNpc: String? = null
            if (!state.showDialog) {
                var bestDistSq = interactRadiusSq
                for (npc in NpcRegistry.npcs) {
                    if (npc.mapId != currentMap) continue
                    val npcState = state.npcStates[npc.id] ?: continue
                    val dx2 = npcState.x - newX
                    val dy2 = npcState.y - newY
                    val distSq = dx2 * dx2 + dy2 * dy2
                    if (distSq <= bestDistSq) {
                        bestDistSq = distSq
                        nearbyNpc = npc.id
                    }
                }
            } else {
                nearbyNpc = state.nearbyNpcId
            }

            // 4. Actualizar partículas (Lluvia) — solo en la calle, no dentro de las casas
            val updatedParticles = if (GameWorld.isInterior(currentMap)) {
                emptyList()
            } else if (state.particles.isEmpty()) {
                // Generar 50 gotas (era 100, reducido para rendimiento)
                List(50) {
                    com.example.cybertura.game.engine.Particle(
                        x = it / 50f,                         // distribuidas uniformemente en X
                        y = (0..1000).random() / 1000f,
                        speedY = (4..12).random() / 1000f,
                        size = (2..5).random().toFloat(),
                        alpha = (2..5).random() / 10f
                    )
                }
            } else {
                // Mover partículas (sin .copy innecesario cuando x no cambia)
                state.particles.map { p ->
                    val newPY = if (p.y + p.speedY > 1f) 0f else p.y + p.speedY
                    val newPX = if (p.x + 0.0005f > 1f) 0f else p.x + 0.0005f
                    p.copy(x = newPX, y = newPY)
                }
            }

            // 5. Animación de caminar
            var walkFrame = state.walkFrame
            if (isWalking) {
                val timeSinceLastFrame = currentTime % (GameState.WALK_ANIM_INTERVAL * 2)
                walkFrame = if (timeSinceLastFrame > GameState.WALK_ANIM_INTERVAL) 1 else 0
            } else {
                walkFrame = 0
            }

            // 6. IA de NPCs (Movimiento libre)
            val dtMs = currentTime - state.lastFrameTimeMs
            val updatedNpcs = state.npcStates.mapValues { (_, npc) ->
                val mapOfNpc = npcMaps[npc.id] ?: MapIds.EXTERIOR
                // Si está en diálogo, no se mueve
                if (state.showDialog && state.nearbyNpcId == npc.id) {
                    return@mapValues npc
                }

                val dx = npc.targetX - npc.x
                val dy = npc.targetY - npc.y
                val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (distance > 0.1f) {
                    // Moverse hacia el objetivo
                    val moveX = (dx / distance) * (GameState.NPC_SPEED * dt)
                    val moveY = (dy / distance) * (GameState.NPC_SPEED * dt)

                    val newNpcX = npc.x + moveX
                    val newNpcY = npc.y + moveY

                    // Solo moverse si no hay colisión
                    if (canMoveTo(mapOfNpc, newNpcX, newNpcY)) {
                        npc.copy(x = newNpcX, y = newNpcY)
                    } else {
                        // Si chocó, abortar este objetivo y forzar nuevo plan
                        npc.copy(targetX = npc.x, targetY = npc.y, waitTimerMs = 1000L)
                    }
                } else {
                    // Llegó al objetivo, restar timer
                    if (npc.waitTimerMs > 0) {
                        npc.copy(waitTimerMs = npc.waitTimerMs - dtMs)
                    } else {
                        // Elegir nuevo destino (dentro de su radio de paseo desde su origen)
                        val randomAngle = Math.random() * 2 * Math.PI
                        val randomRadius = Math.random() * (npcWander[npc.id] ?: 4f)
                        val rawTargetX = (npc.startX + kotlin.math.cos(randomAngle) * randomRadius).toFloat()
                        val rawTargetY = (npc.startY + kotlin.math.sin(randomAngle) * randomRadius).toFloat()

                        // Validar que el nuevo destino sea caminable, si no, se queda quieto
                        if (canMoveTo(mapOfNpc, rawTargetX, rawTargetY)) {
                            npc.copy(
                                targetX = rawTargetX,
                                targetY = rawTargetY,
                                waitTimerMs = (1000..4000).random().toLong() // Esperará 1-4 segundos al llegar
                            )
                        } else {
                            npc.copy(waitTimerMs = 1000L) // Reintenta en 1 segundo
                        }
                    }
                }
            }

            // Devolver nuevo estado
            state.copy(
                playerX = newX,
                playerY = newY,
                playerDirection = direction,
                isWalking = isWalking,
                currentMapId = currentMap,
                joystickX = joyX,
                joystickY = joyY,
                walkFrame = walkFrame,
                nearbyNpcId = nearbyNpc,
                lastFrameTimeMs = currentTime,
                frameCount = state.frameCount + 1,
                particles = updatedParticles,
                npcStates = updatedNpcs
            )
        }
    }

    /**
     * Mueve sobre un solo eje en pasos pequeños y se detiene justo al tocar un obstáculo.
     * Antes, si el paso completo chocaba se descartaba entero, así que el jugador frenaba
     * a una distancia variable de la pared (hasta 1/3 de tile en un frame lento).
     */
    private fun moveAxis(mapId: String, x: Float, y: Float, delta: Float, horizontal: Boolean): Float {
        if (delta == 0f) return if (horizontal) x else y
        val steps = kotlin.math.ceil(abs(delta) / COLLISION_STEP).toInt()
        val step = delta / steps
        var pos = if (horizontal) x else y
        repeat(steps) {
            val next = pos + step
            val free = if (horizontal) canMoveTo(mapId, next, y) else canMoveTo(mapId, x, next)
            if (!free) return pos
            pos = next
        }
        return pos
    }

    /**
     * Si el jugador empuja contra un borde y a menos de medio tile hay un hueco libre,
     * lo desliza en el eje perpendicular hacia ese hueco (como en los RPG clásicos).
     */
    private fun cornerAssist(
        mapId: String, x: Float, y: Float, push: Float, speed: Float, horizontal: Boolean
    ): Float {
        val perp = if (horizontal) y else x
        val aligned = kotlin.math.round(perp)
        val offset = aligned - perp
        if (abs(offset) < 0.01f || abs(offset) > 0.45f) return perp
        val probe = kotlin.math.sign(push) * COLLISION_STEP
        val gapIsFree = if (horizontal) canMoveTo(mapId, x + probe, aligned) else canMoveTo(mapId, aligned, y + probe)
        if (!gapIsFree) return perp
        val nudge = offset.coerceIn(-speed, speed)
        val target = perp + nudge
        val canSlide = if (horizontal) canMoveTo(mapId, x, target) else canMoveTo(mapId, target, y)
        return if (canSlide) target else perp
    }

    /**
     * Comprueba si se puede pisar la posición (x, y) del mapa [mapId].
     * La caja de colisión cubre los pies y el cuerpo (no la cabeza), alineada con la
     * sombra del sprite, para que el choque coincida con lo que se ve en pantalla.
     */
    private fun canMoveTo(mapId: String, x: Float, y: Float): Boolean {
        val left = floor(x + HITBOX_LEFT).toInt()
        val right = floor(x + HITBOX_RIGHT).toInt()
        val top = floor(y + HITBOX_TOP).toInt()
        val bottom = floor(y + HITBOX_BOTTOM).toInt()

        return GameWorld.isWalkable(mapId, left, top) &&
               GameWorld.isWalkable(mapId, right, top) &&
               GameWorld.isWalkable(mapId, left, bottom) &&
               GameWorld.isWalkable(mapId, right, bottom)
    }

    // --- Controles Públicos ---

    fun updateJoystick(x: Float, y: Float) {
        // Limitar vector máximo a 1 (para que en diagonal no vaya más rápido)
        val mag = sqrt(x * x + y * y)
        if (mag > 1f) {
            _gameState.update { it.copy(joystickX = x / mag, joystickY = y / mag) }
        } else {
            _gameState.update { it.copy(joystickX = x, joystickY = y) }
        }
    }

    fun interact() {
        val state = _gameState.value

        // Si ya hay un diálogo mostrándose
        if (state.showDialog) {
            if (state.dialogFullyRevealed) {
                // Aquí el UI debería navegar a la pantalla de la misión
            } else {
                // Acelerar/mostrar texto completo (implementado en UI)
            }
            return
        }

        // Si hay un NPC cerca y presionamos interactuar
        if (state.nearbyNpcId != null) {
            val npc = NpcRegistry.getNpcById(state.nearbyNpcId)
            if (npc != null) {
                // Mirar hacia el NPC (usando su posición actual, no la inicial)
                val npcLive = state.npcStates[npc.id]
                val npcX = npcLive?.x ?: npc.tileX
                val npcY = npcLive?.y ?: npc.tileY
                val dir = if (abs(npcX - state.playerX) > abs(npcY - state.playerY)) {
                    if (npcX > state.playerX) Direction.RIGHT else Direction.LEFT
                } else {
                    if (npcY > state.playerY) Direction.DOWN else Direction.UP
                }

                var finalDialog = npc.greetingDialog
                var isBlocked = false

                if (npc.questOrder != null) {
                    val currentTargetId = state.nextQuestNpcId
                    
                    if (currentTargetId == null) {
                        // Ya completó el juego entero
                        finalDialog = npc.completedDialog
                        isBlocked = true
                    } else if (currentTargetId != npc.id) {
                        val targetNpc = NpcRegistry.getNpcById(currentTargetId)
                        // Se decide por los módulos completados y no por el número de orden:
                        // si el orden de la misión cambia, una partida guardada puede tener
                        // completado un maestro que ahora va después del objetivo actual.
                        if (npc.moduleId in completedModuleIds) {
                            // Este NPC ya fue completado
                            finalDialog = npc.completedDialog
                            isBlocked = true
                        } else if (targetNpc != null) {
                            // Este NPC aún no está listo
                            finalDialog = "Todavía no es tu momento para mi reto. Tu siguiente destino es ${targetNpc.name}: lo encontrarás ${targetNpc.locationHint.ifBlank { "en la ciudad" }}. ¡Sigue el ❗ dorado!"
                            isBlocked = true
                        }
                    }
                }

                _gameState.update {
                    it.copy(
                        playerDirection = dir,
                        showDialog = true,
                        dialogNpcId = npc.id,
                        dialogText = finalDialog,
                        dialogIsBlocked = isBlocked,
                        dialogCharIndex = 0,
                        dialogFullyRevealed = false
                    )
                }
            }
        }
    }

    fun closeDialog() {
        _gameState.update {
            it.copy(showDialog = false, dialogNpcId = null)
        }
    }

    companion object {
        // Caja de colisión relativa a la esquina superior izquierda del sprite (en tiles)
        private const val HITBOX_LEFT = 0.25f
        private const val HITBOX_RIGHT = 0.75f
        private const val HITBOX_TOP = 0.35f
        private const val HITBOX_BOTTOM = 0.95f
        /** Paso máximo al resolver colisiones: el jugador queda a ≤ esta distancia del obstáculo. */
        private const val COLLISION_STEP = 0.05f

        fun provideFactory(repository: CyberRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameWorldViewModel(repository) as T
                }
            }
    }
}
