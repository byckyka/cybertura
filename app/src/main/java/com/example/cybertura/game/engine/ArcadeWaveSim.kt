package com.example.cybertura.game.engine

import com.example.cybertura.data.model.ArcadeItem
import com.example.cybertura.data.model.ArcadeWave
import kotlin.random.Random

/** Objeto que cae durante una oleada del arcade. */
class ArcadeSpawn(
    val uid: Int,
    val item: ArcadeItem,
    /** Carril por el que cae: 0 izquierda, 1 centro, 2 derecha. */
    val lane: Int,
    /** Momento de la oleada (ms) en que aparece arriba. */
    val spawnAt: Long
)

enum class ArcadeOutcome(val tapped: Boolean, val losesShield: Boolean) {
    /** Tocó una amenaza: bien. */
    DESTROYED(tapped = true, losesShield = false),
    /** Dejó pasar algo seguro: bien. */
    PASSED(tapped = false, losesShield = false),
    /** Dejó pasar una amenaza: pierde un escudo. */
    HIT(tapped = false, losesShield = true),
    /** Tocó algo seguro: pierde un escudo. */
    WRONG_TAP(tapped = true, losesShield = true)
}

/** Lo que pasó con un objeto: la pantalla lo usa para sonido y mensaje; el piloto, para el registro. */
class ArcadeEvent(val spawn: ArcadeSpawn, val outcome: ArcadeOutcome)

/**
 * Simulación de una oleada de "Defiende el bosque", sin nada de interfaz.
 *
 * La pantalla solo la hace avanzar cuadro a cuadro ([advance]) y le avisa de los toques ([tap]).
 * Vive en el ViewModel: si la pantalla se recrea (por ejemplo al girar el teléfono), la oleada
 * sigue donde iba en vez de empezar de nuevo.
 */
class ArcadeWaveSim(
    wave: ArcadeWave,
    pool: List<ArcadeItem>,
    val maxShields: Int,
    random: Random = Random.Default
) {
    /** Milisegundos que tarda un objeto en caer hasta el bosque. */
    val fallMs: Long = wave.fallMs ?: DEFAULT_FALL_MS

    val spawns: List<ArcadeSpawn> = buildWave(wave, pool, random)

    private val outcomes = mutableMapOf<Int, ArcadeOutcome>()

    /** Tiempo de juego transcurrido en la oleada. */
    var elapsedMs: Long = 0L
        private set

    var shields: Int = maxShields
        private set

    val resolvedCount: Int get() = outcomes.size

    /** Se acabaron los escudos: la oleada se pierde y hay que repetirla. */
    val failed: Boolean get() = shields <= 0

    val isOver: Boolean get() = failed || outcomes.size == spawns.size

    /** Escudos perdidos en la oleada; si se perdió, cuentan todos. */
    val shieldsLost: Int get() = if (failed) maxShields else maxShields - shields

    fun isResolved(uid: Int): Boolean = uid in outcomes

    /** El objeto ya apareció y sigue cayendo. */
    fun isFalling(spawn: ArcadeSpawn): Boolean = spawn.uid !in outcomes && elapsedMs >= spawn.spawnAt

    /** Avance de la caída de un objeto, de 0 (arriba) a 1 (llegó al bosque). */
    fun progress(spawn: ArcadeSpawn): Float =
        ((elapsedMs - spawn.spawnAt).toFloat() / fallMs).coerceIn(0f, 1f)

    /** Avanza el tiempo y devuelve los objetos que llegaron al bosque en este paso. */
    fun advance(dtMs: Long): List<ArcadeEvent> {
        if (isOver || dtMs <= 0L) return emptyList()
        elapsedMs += dtMs
        val events = mutableListOf<ArcadeEvent>()
        for (spawn in spawns) {
            // Sin escudos la oleada ya se perdió: lo que siga cayendo no cuenta
            if (failed) break
            if (spawn.uid in outcomes || elapsedMs < spawn.spawnAt + fallMs) continue
            events += resolve(spawn, if (spawn.item.isThreat) ArcadeOutcome.HIT else ArcadeOutcome.PASSED)
        }
        return events
    }

    /** El niño tocó un objeto. Devuelve null si el toque no cuenta (ya resuelto o aún no aparece). */
    fun tap(uid: Int): ArcadeEvent? {
        if (isOver) return null
        val spawn = spawns.firstOrNull { it.uid == uid } ?: return null
        if (!isFalling(spawn)) return null
        return resolve(spawn, if (spawn.item.isThreat) ArcadeOutcome.DESTROYED else ArcadeOutcome.WRONG_TAP)
    }

    private fun resolve(spawn: ArcadeSpawn, outcome: ArcadeOutcome): ArcadeEvent {
        outcomes[spawn.uid] = outcome
        if (outcome.losesShield) shields -= 1
        return ArcadeEvent(spawn, outcome)
    }

    companion object {
        const val DEFAULT_FALL_MS = 6000L
        const val DEFAULT_SPAWN_MS = 1200L
        /** Pausa antes del primer objeto, para que el niño alcance a mirar. */
        const val FIRST_SPAWN_MS = 600L
        const val THREAT_RATIO = 0.6f
        const val LANES = 3

        /** Arma los objetos de la oleada: ~60 % amenazas, sin repetir carril dos veces seguidas. */
        fun buildWave(wave: ArcadeWave, pool: List<ArcadeItem>, random: Random = Random.Default): List<ArcadeSpawn> {
            val threats = pool.filter { it.isThreat }.shuffled(random)
            val safes = pool.filter { !it.isThreat }.shuffled(random)
            if (threats.isEmpty() && safes.isEmpty()) return emptyList()
            val spawnMs = wave.spawnMs ?: DEFAULT_SPAWN_MS
            var lastLane = -1
            return List(wave.count.coerceAtLeast(0)) { i ->
                val useThreat = safes.isEmpty() || (threats.isNotEmpty() && random.nextFloat() < THREAT_RATIO)
                val item = if (useThreat) threats[i % threats.size] else safes[i % safes.size]
                val lane = (0 until LANES).filter { it != lastLane }.random(random).also { lastLane = it }
                ArcadeSpawn(uid = i, item = item, lane = lane, spawnAt = FIRST_SPAWN_MS + i * spawnMs)
            }
        }
    }
}
