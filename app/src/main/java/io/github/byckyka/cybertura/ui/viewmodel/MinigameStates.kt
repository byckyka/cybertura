package io.github.byckyka.cybertura.ui.viewmodel

import io.github.byckyka.cybertura.data.model.ArcadeChallenge
import io.github.byckyka.cybertura.data.model.ArcadeWave
import io.github.byckyka.cybertura.data.model.ChatStep
import io.github.byckyka.cybertura.data.model.ChatStory
import io.github.byckyka.cybertura.data.model.Clue
import io.github.byckyka.cybertura.data.model.ForgeBlock
import io.github.byckyka.cybertura.data.model.ForgeRound
import io.github.byckyka.cybertura.data.model.SwipeCard
import io.github.byckyka.cybertura.game.engine.ArcadeWaveSim

// Estado de cada minijuego. Las reglas que lo hacen avanzar están en MinigameRules.kt.

enum class ArcadePhase {
    /** Explicación antes de la primera oleada. */
    INTRO,
    PLAYING,
    /** Oleada superada: se muestra su consejo. */
    WAVE_CLEAR,
    /** Se acabaron los escudos: la oleada se repite. */
    WAVE_FAILED
}

data class ArcadeGameState(
    val config: ArcadeChallenge,
    val waveIndex: Int = 0,
    val phase: ArcadePhase = ArcadePhase.INTRO,
    /** Intentos de oleada: clave para reiniciar la simulación en pantalla. */
    val attempt: Int = 0,
    val shieldsLostTotal: Int = 0,
    val wavesFailed: Int = 0,
    /** Consejo de la última oleada superada. */
    val lastTip: String? = null,
    /** Se superó la última oleada. */
    val finished: Boolean = false,
    /**
     * Oleada en curso (solo en fase PLAYING). Es un objeto vivo que la pantalla hace avanzar;
     * se guarda aquí para que la oleada no se reinicie si la pantalla se recrea.
     */
    val sim: ArcadeWaveSim? = null
) {
    val shields: Int get() = (config.shields ?: 3).coerceAtLeast(1)
    val wave: ArcadeWave get() = config.waves[waveIndex]
    /** Cada oleada perdida pesa como 2 escudos perdidos al calcular las estrellas. */
    val stars: Int get() = (shieldsLostTotal + wavesFailed * 2).let { penalty ->
        when {
            penalty <= 1 -> 3
            penalty <= 4 -> 2
            else -> 1
        }
    }
}

enum class ForgePhase {
    /** Armando la llave con bloques. */
    EDITING,
    /** Eligiendo qué hacer ante el ladrón. */
    DEFENDING,
    SUCCESS,
    FAIL
}

data class ForgeGameState(
    val rounds: List<ForgeRound>,
    val roundIndex: Int = 0,
    /** Bloques puestos en la llave, en orden. */
    val selectedIds: List<String> = emptyList(),
    val phase: ForgePhase,
    val resultTitle: String? = null,
    val resultMessage: String? = null,
    val mistakes: Int = 0,
    /** Opciones de defensa ya descartadas en la ronda actual. */
    val disabledOptionIds: Set<String> = emptySet(),
    /** Llaves forjadas con éxito (se muestran en las rondas de defensa). */
    val forgedKeys: List<String> = emptyList(),
    /** Contador de resultados: clave para sonido y animación una vez por resultado. */
    val eventCount: Int = 0
) {
    val round: ForgeRound get() = rounds[roundIndex]
    val isLastRound: Boolean get() = roundIndex == rounds.lastIndex
    val selectedBlocks: List<ForgeBlock>
        get() = selectedIds.mapNotNull { id -> round.blocks?.firstOrNull { it.id == id } }
    val key: String get() = selectedBlocks.joinToString("") { it.text }
    val stars: Int get() = when {
        mistakes == 0 -> 3
        mistakes <= 2 -> 2
        else -> 1
    }

    companion object {
        const val MAX_BLOCKS = 8
        fun phaseFor(round: ForgeRound) = if (round.isDefend) ForgePhase.DEFENDING else ForgePhase.EDITING
    }
}

enum class ChatLineKind {
    /** Mensaje de otra persona del grupo. */
    OTHER,
    /** Lo que escribió el jugador. */
    PLAYER,
    /** Nota del sistema (ej. "Chat privado con Mateo"). */
    NOTE,
    /** Explicación de una buena decisión. */
    GOOD_TIP
}

data class ChatLine(
    val id: Int,
    val kind: ChatLineKind,
    val text: String,
    val from: String? = null,
    val emoji: String? = null,
    /** Mensaje del jugador que se "deshizo" para volver a elegir. */
    val retracted: Boolean = false
)

enum class ChatPhase { PLAYING, CHOOSING, RETRY, DONE }

data class ChatGameState(
    val story: ChatStory,
    val stepIndex: Int = 0,
    val lines: List<ChatLine> = emptyList(),
    /** Quién está "escribiendo…" ahora mismo. */
    val typingFrom: String? = null,
    val phase: ChatPhase = ChatPhase.PLAYING,
    val meter: Int,
    val mistakes: Int = 0,
    /** Opciones del paso actual ya descartadas (se eligieron y no ayudaban). */
    val disabledChoiceIds: Set<String> = emptySet(),
    /** Orden barajado de las opciones del paso actual, para que la buena no esté siempre en el mismo lugar. */
    val choiceOrder: List<String> = emptyList(),
    val retryFeedback: String? = null,
    val lastBadLineId: Int? = null
) {
    val currentStep: ChatStep? get() = story.steps.getOrNull(stepIndex)
    val meterIcons: List<String> get() = story.meterIcons?.takeIf { it.size >= 2 } ?: DEFAULT_METER_ICONS
    val stars: Int get() = when (mistakes) {
        0 -> 3
        1 -> 2
        else -> 1
    }

    companion object {
        val DEFAULT_METER_ICONS = listOf("😭", "😢", "😟", "🙂", "😄")
    }
}

/** Último toque en la escena del detective (para sonido, vibración y mensajes). */
data class SpotEvent(
    /** Pista encontrada, o null si tocó algo que no era sospechoso. */
    val clueId: String?,
    /** Fue la última pista: el modal de victoria se encarga del sonido. */
    val isFinal: Boolean = false
)

data class SpotGameState(
    val clues: List<Clue>,
    /** Pistas encontradas, en el orden en que se descubrieron. */
    val foundIds: List<String> = emptyList(),
    val misses: Int = 0,
    val hintsUsed: Int = 0,
    /** Pista cuya ayuda está visible ahora mismo. */
    val hintClueId: String? = null,
    /** Contador de toques: clave para disparar sonido/animación una vez por toque. */
    val eventCount: Int = 0,
    val lastEvent: SpotEvent? = null
) {
    val total: Int get() = clues.size
    /** Cada ayuda cuenta como 2 toques fallidos al calcular las estrellas. */
    val stars: Int get() = (misses + hintsUsed * 2).let { penalty ->
        when {
            penalty <= 1 -> 3
            penalty <= 4 -> 2
            else -> 1
        }
    }
}

/** Resultado de la última carta deslizada (para animaciones, sonido y explicación). */
data class SwipeResult(
    val card: SwipeCard,
    val wasCorrect: Boolean,
    /** Fue la última carta: el modal de victoria se encarga del sonido. */
    val isFinal: Boolean
)

data class SwipeGameState(
    /** Cartas pendientes; la primera es la que está arriba del mazo. */
    val queue: List<SwipeCard>,
    val total: Int,
    val sorted: Int = 0,
    val mistakes: Int = 0,
    val streak: Int = 0,
    /** Contador de deslizamientos: sirve de clave para reiniciar la animación de cada carta. */
    val swipeCount: Int = 0,
    val lastResult: SwipeResult? = null,
    /** Tras un fallo, el mazo se bloquea hasta que el niño lea la explicación. */
    val awaitingAck: Boolean = false
) {
    val stars: Int get() = when {
        mistakes == 0 -> 3
        mistakes <= 2 -> 2
        else -> 1
    }
}
