package com.example.cybertura.game.engine

/**
 * Dirección en la que mira el personaje / se mueve.
 */
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

/**
 * Estado mutable del juego en cada frame.
 */
data class GameState(
    // Posición del jugador en coordenadas de tile (con decimales para movimiento suave)
    val playerX: Float = GameWorld.playerSpawnX,
    val playerY: Float = GameWorld.playerSpawnY,
    val playerDirection: Direction = Direction.DOWN,
    val isWalking: Boolean = false,

    // Mapa en el que está el jugador (calle o interior de una casa)
    val currentMapId: String = MapIds.EXTERIOR,
    val walkFrame: Int = 0, // alterna 0/1 para animación de caminar

    // Input del joystick normalizado (-1 a 1)
    val joystickX: Float = 0f,
    val joystickY: Float = 0f,

    // NPC cercano al jugador (null si no hay ninguno cerca)
    val nearbyNpcId: String? = null,

    // Estado de diálogo con NPC
    val showDialog: Boolean = false,
    val dialogNpcId: String? = null,
    val dialogText: String = "",
    val dialogCharIndex: Int = 0, // para animación letra-por-letra
    val dialogFullyRevealed: Boolean = false,
    val dialogIsBlocked: Boolean = false,

    // Animación y físicas del clima/partículas
    val lastFrameTimeMs: Long = 0L,
    val frameCount: Long = 0L,

    // Partículas para el clima (Lluvia digital)
    val particles: List<Particle> = emptyList(),

    // Estado dinámico de los NPCs (Para que puedan caminar)
    val npcStates: Map<String, NpcState> = emptyMap(),

    /** ID del siguiente NPC de misión que el jugador debe visitar (para mostrar marcador !) */
    val nextQuestNpcId: String? = null
) {
    companion object {
        const val PLAYER_SPEED = 3.5f // tiles por segundo
        const val NPC_SPEED = 1.5f // NPCs caminan más lento
        const val NPC_INTERACT_RADIUS = 1.8f // tiles de distancia para interactuar
        const val WALK_ANIM_INTERVAL = 250L // ms entre frames de animación
        const val DIALOG_CHAR_SPEED = 35L // ms entre cada letra del diálogo
    }
}

/**
 * Representa el "cerebro" y posición de un NPC en vivo.
 */
data class NpcState(
    val id: String,
    val x: Float,
    val y: Float,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val waitTimerMs: Long
)

/**
 * Representa una partícula individual para el sistema de clima.
 */
data class Particle(
    val x: Float,
    val y: Float,
    val speedY: Float,
    val size: Float,
    val alpha: Float
)
