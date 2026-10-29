package com.example.cybertura.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cybertura.data.model.ArcadeChallenge
import com.example.cybertura.data.model.ArcadeWave
import com.example.cybertura.data.model.Challenge
import com.example.cybertura.data.model.ChatMessage
import com.example.cybertura.data.model.ChatStep
import com.example.cybertura.data.model.ChatStory
import com.example.cybertura.data.model.Clue
import com.example.cybertura.data.model.ForgeBlock
import com.example.cybertura.data.model.ForgeRound
import com.example.cybertura.game.engine.KeyStrength
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.Option
import com.example.cybertura.data.model.SwipeCard
import com.example.cybertura.data.repository.CyberRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameplayUiState(
    val module: CyberModule? = null,
    val challenge: Challenge? = null,
    val challengeIndex: Int = 0,
    val totalChallenges: Int = 0,
    val selectedOption: Option? = null,
    val showFeedback: Boolean = false,
    val isCorrect: Boolean = false,
    val feedbackText: String = "",
    val educationalTip: String = "",
    val isChallengeCompleted: Boolean = false,
    val isModuleFinished: Boolean = false,
    val nextChallengeId: String? = null,
    /** Título del modal final si el reto lo define (minijuegos); null = título por defecto. */
    val resultTitle: String? = null,
    val swipe: SwipeGameState? = null,
    val spot: SpotGameState? = null,
    val chat: ChatGameState? = null,
    val forge: ForgeGameState? = null,
    val arcade: ArcadeGameState? = null
)

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
    val finished: Boolean = false
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

class GameplayViewModel(
    private val repository: CyberRepository,
    private val moduleId: String,
    private val challengeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameplayUiState())
    val uiState: StateFlow<GameplayUiState> = _uiState.asStateFlow()

    init {
        loadChallenge()
    }

    private fun loadChallenge() {
        val module = repository.getModuleById(moduleId)
        val challenge = repository.getChallenge(moduleId, challengeId)
        if (module != null && challenge != null) {
            val index = module.challenges.indexOfFirst { it.id == challengeId }
            val nextChallenge = module.challenges.getOrNull(index + 1)
            val isLast = index == module.challenges.size - 1

            _uiState.update {
                it.copy(
                    module = module,
                    challenge = challenge,
                    challengeIndex = index + 1,
                    totalChallenges = module.challenges.size,
                    nextChallengeId = nextChallenge?.id,
                    isModuleFinished = isLast,
                    swipe = challenge.cards
                        ?.takeIf { challenge.type == Challenge.TYPE_SWIPE_SORT && it.isNotEmpty() }
                        ?.let { cards -> SwipeGameState(queue = cards.shuffled(), total = cards.size) },
                    spot = challenge.clues
                        ?.takeIf { challenge.type == Challenge.TYPE_SPOT_THE_CLUES && it.isNotEmpty() && challenge.scene != null }
                        ?.let { clues -> SpotGameState(clues = clues) },
                    chat = challenge.chat
                        ?.takeIf { challenge.type == Challenge.TYPE_CHAT_STORY && it.steps.isNotEmpty() }
                        ?.let { story ->
                            val icons = story.meterIcons?.takeIf { it.size >= 2 } ?: ChatGameState.DEFAULT_METER_ICONS
                            ChatGameState(story = story, meter = (story.meterStart ?: 1).coerceIn(0, icons.size - 1))
                        },
                    forge = challenge.forge?.rounds
                        ?.takeIf { challenge.type == Challenge.TYPE_FORGE_KEY && it.isNotEmpty() }
                        ?.let { rounds -> ForgeGameState(rounds = rounds, phase = ForgeGameState.phaseFor(rounds.first())) },
                    arcade = challenge.arcade
                        ?.takeIf { challenge.type == Challenge.TYPE_TAP_DEFENSE && it.waves.isNotEmpty() && it.items.isNotEmpty() }
                        ?.let { ArcadeGameState(config = it) }
                )
            }
            if (_uiState.value.chat != null) {
                viewModelScope.launch { playChatStep(0) }
            }
        }
    }

    // ── Minijuego del chat de grupo ──────────────────────────────────────────

    private fun updateChat(transform: (ChatGameState) -> ChatGameState) {
        _uiState.update { state -> state.chat?.let { state.copy(chat = transform(it)) } ?: state }
    }

    private fun appendLine(kind: ChatLineKind, text: String, from: String? = null, emoji: String? = null): Int {
        var id = 0
        updateChat { chat ->
            id = chat.lines.size
            chat.copy(lines = chat.lines + ChatLine(id, kind, text, from, emoji))
        }
        return id
    }

    /** Muestra los mensajes uno a uno, con "está escribiendo…" y pausas como en un chat real. */
    private suspend fun revealMessages(messages: List<ChatMessage>?) {
        for (message in messages.orEmpty()) {
            if (message.from != null) {
                updateChat { it.copy(typingFrom = message.from) }
                delay((700L + message.text.length * 15L).coerceAtMost(1800L))
                updateChat { it.copy(typingFrom = null) }
                appendLine(ChatLineKind.OTHER, message.text, message.from, message.emoji)
            } else {
                delay(400)
                appendLine(ChatLineKind.NOTE, message.text)
            }
            delay(350)
        }
    }

    private suspend fun playChatStep(index: Int) {
        val story = _uiState.value.chat?.story ?: return
        val step = story.steps.getOrNull(index) ?: return
        updateChat {
            it.copy(
                stepIndex = index,
                phase = ChatPhase.PLAYING,
                disabledChoiceIds = emptySet(),
                choiceOrder = step.choices.orEmpty().map { c -> c.id }.shuffled()
            )
        }
        revealMessages(step.messages)
        if (step.choices.isNullOrEmpty()) {
            // Paso sin opciones: es el final de la historia
            updateChat { it.copy(phase = ChatPhase.DONE) }
            val challenge = _uiState.value.challenge ?: return
            val chat = _uiState.value.chat ?: return
            delay(900)
            finishChatGame(challenge, chat)
        } else {
            updateChat { it.copy(phase = ChatPhase.CHOOSING) }
        }
    }

    /** El jugador eligió qué responder en el chat. */
    fun chooseChatReply(choiceId: String) {
        val chat = _uiState.value.chat ?: return
        if (chat.phase != ChatPhase.CHOOSING) return
        val choice = chat.currentStep?.choices?.firstOrNull { it.id == choiceId } ?: return
        if (choice.id in chat.disabledChoiceIds) return

        val maxMeter = chat.meterIcons.size - 1
        updateChat {
            it.copy(
                phase = ChatPhase.PLAYING,
                meter = (it.meter + if (choice.isGood) 1 else -1).coerceIn(0, maxMeter),
                mistakes = if (choice.isGood) it.mistakes else it.mistakes + 1
            )
        }
        val lineId = appendLine(ChatLineKind.PLAYER, choice.text)

        viewModelScope.launch {
            delay(400)
            revealMessages(choice.reactions)
            if (choice.isGood) {
                appendLine(ChatLineKind.GOOD_TIP, choice.feedback)
                delay(1200)
                playChatStep(chat.stepIndex + 1)
            } else {
                updateChat {
                    it.copy(
                        phase = ChatPhase.RETRY,
                        retryFeedback = choice.feedback,
                        lastBadLineId = lineId,
                        disabledChoiceIds = it.disabledChoiceIds + choice.id
                    )
                }
            }
        }
    }

    /** Tras leer la explicación de una mala decisión, el jugador vuelve a elegir. */
    fun retryChatReply() {
        val chat = _uiState.value.chat ?: return
        if (chat.phase != ChatPhase.RETRY) return
        updateChat { state ->
            state.copy(
                lines = state.lines.map { if (it.id == state.lastBadLineId) it.copy(retracted = true) else it },
                phase = ChatPhase.CHOOSING,
                retryFeedback = null,
                lastBadLineId = null
            )
        }
        appendLine(ChatLineKind.NOTE, "⏪ Volvamos a intentarlo")
    }

    // ── Minijuego de la forja de llaves ──────────────────────────────────────

    private fun updateForge(transform: (ForgeGameState) -> ForgeGameState) {
        _uiState.update { state -> state.forge?.let { state.copy(forge = transform(it)) } ?: state }
    }

    /** Pone o quita un bloque de la llave. */
    fun toggleForgeBlock(blockId: String) {
        updateForge { forge ->
            if (forge.phase != ForgePhase.EDITING) return@updateForge forge
            when {
                blockId in forge.selectedIds -> forge.copy(selectedIds = forge.selectedIds - blockId)
                forge.selectedIds.size >= ForgeGameState.MAX_BLOCKS -> forge
                else -> forge.copy(selectedIds = forge.selectedIds + blockId)
            }
        }
    }

    /** El ladrón pone a prueba la llave armada. */
    fun forgeKey() {
        updateForge { forge ->
            if (forge.phase != ForgePhase.EDITING || forge.selectedIds.isEmpty()) return@updateForge forge
            val key = forge.key
            val level = KeyStrength.level(key)
            val goal = (forge.round.minLevel ?: 2).coerceIn(0, KeyStrength.MAX_LEVEL)
            val trap = forge.selectedBlocks.firstOrNull { it.isTrap }
            when {
                trap != null -> forge.copy(
                    phase = ForgePhase.FAIL,
                    mistakes = forge.mistakes + 1,
                    // Quitamos el bloque trampa para que el niño vea qué falló y pueda reintentar
                    selectedIds = forge.selectedIds - trap.id,
                    resultTitle = "💥 ¡El ladrón la adivinó!",
                    resultMessage = (trap.feedback ?: "Ese dato es muy fácil de adivinar.") +
                        " Quitamos \"${trap.text}\" de tu llave.",
                    eventCount = forge.eventCount + 1
                )
                level < goal -> {
                    val material = KeyStrength.MATERIALS[level]
                    val needed = KeyStrength.MATERIALS[goal]
                    forge.copy(
                        phase = ForgePhase.FAIL,
                        mistakes = forge.mistakes + 1,
                        resultTitle = "💥 ¡La llave se rompió!",
                        resultMessage = "Tu llave es de ${material.emoji} ${material.name} y el ladrón la forzó. " +
                            "Necesitas al menos ${needed.emoji} ${needed.name}. " +
                            (KeyStrength.tips(key).firstOrNull() ?: ""),
                        eventCount = forge.eventCount + 1
                    )
                }
                else -> {
                    val material = KeyStrength.MATERIALS[level]
                    forge.copy(
                        phase = ForgePhase.SUCCESS,
                        forgedKeys = forge.forgedKeys + key,
                        resultTitle = "🛡️ ¡La llave resistió!",
                        resultMessage = "Forjaste una llave de ${material.emoji} ${material.name}. " +
                            "El ladrón lo intentó con todas sus fuerzas… ¡y no pudo abrirla!",
                        eventCount = forge.eventCount + 1
                    )
                }
            }
        }
    }

    /** El niño decide qué hacer con su llave ante el ladrón. */
    fun chooseForgeOption(optionId: String) {
        updateForge { forge ->
            if (forge.phase != ForgePhase.DEFENDING || optionId in forge.disabledOptionIds) return@updateForge forge
            val option = forge.round.options?.firstOrNull { it.id == optionId } ?: return@updateForge forge
            if (option.isGood) {
                forge.copy(
                    phase = ForgePhase.SUCCESS,
                    resultTitle = "🛡️ ¡Llave protegida!",
                    resultMessage = option.feedback,
                    eventCount = forge.eventCount + 1
                )
            } else {
                forge.copy(
                    phase = ForgePhase.FAIL,
                    mistakes = forge.mistakes + 1,
                    disabledOptionIds = forge.disabledOptionIds + option.id,
                    resultTitle = "🦹 ¡El ladrón se llevó tu llave!",
                    resultMessage = option.feedback,
                    eventCount = forge.eventCount + 1
                )
            }
        }
    }

    /** Botón del panel de resultado: siguiente ronda (tras un éxito) o reintentar (tras un fallo). */
    fun continueForge() {
        val forge = _uiState.value.forge ?: return
        when (forge.phase) {
            ForgePhase.SUCCESS -> {
                if (forge.isLastRound) {
                    val challenge = _uiState.value.challenge ?: return
                    finishForgeGame(challenge, forge)
                } else {
                    val next = forge.rounds[forge.roundIndex + 1]
                    updateForge {
                        it.copy(
                            roundIndex = it.roundIndex + 1,
                            selectedIds = emptyList(),
                            disabledOptionIds = emptySet(),
                            phase = ForgeGameState.phaseFor(next),
                            resultTitle = null,
                            resultMessage = null
                        )
                    }
                }
            }
            ForgePhase.FAIL -> updateForge {
                it.copy(phase = ForgeGameState.phaseFor(it.round), resultTitle = null, resultMessage = null)
            }
            else -> Unit
        }
    }

    private fun finishForgeGame(challenge: Challenge, forge: ForgeGameState) {
        val title = when (forge.stars) {
            3 -> "¡Maestro herrero!"
            2 -> "¡Llaves indestructibles!"
            else -> "¡Lo lograste, herrero!"
        }
        val summary = when (forge.mistakes) {
            0 -> "Forjaste y defendiste todas tus llaves sin un solo error. ¡Ningún ladrón podrá contigo!"
            1 -> "Solo una llave se te escapó, pero la arreglaste enseguida. ¡Muy bien!"
            else -> "El ladrón te ganó ${forge.mistakes} veces, pero aprendiste de cada intento y tus llaves quedaron indestructibles."
        }
        finishMinigame(challenge, forge.stars, title, summary)
    }

    // ── Arcade "Defiende el bosque" ──────────────────────────────────────────

    /** Empieza (o repite) la oleada actual. */
    fun startArcadeWave() {
        _uiState.update { state ->
            val arcade = state.arcade ?: return@update state
            if (arcade.phase == ArcadePhase.PLAYING || arcade.finished) return@update state
            state.copy(arcade = arcade.copy(phase = ArcadePhase.PLAYING, attempt = arcade.attempt + 1))
        }
    }

    /** La pantalla avisa que terminó la oleada: [failed] si se acabaron los escudos. */
    fun onArcadeWaveEnded(shieldsLost: Int, failed: Boolean) {
        val state = _uiState.value
        val arcade = state.arcade ?: return
        if (arcade.phase != ArcadePhase.PLAYING) return
        val updated = arcade.copy(shieldsLostTotal = arcade.shieldsLostTotal + shieldsLost)
        when {
            failed -> updateArcade { updated.copy(phase = ArcadePhase.WAVE_FAILED, wavesFailed = arcade.wavesFailed + 1) }
            arcade.waveIndex == arcade.config.waves.lastIndex -> {
                updateArcade { updated.copy(phase = ArcadePhase.WAVE_CLEAR, lastTip = arcade.wave.tip, finished = true) }
                val challenge = state.challenge ?: return
                finishArcadeGame(challenge, updated)
            }
            else -> updateArcade {
                updated.copy(phase = ArcadePhase.WAVE_CLEAR, waveIndex = arcade.waveIndex + 1, lastTip = arcade.wave.tip)
            }
        }
    }

    private fun updateArcade(transform: (ArcadeGameState) -> ArcadeGameState) {
        _uiState.update { state -> state.arcade?.let { state.copy(arcade = transform(it)) } ?: state }
    }

    private fun finishArcadeGame(challenge: Challenge, arcade: ArcadeGameState) {
        val title = when (arcade.stars) {
            3 -> "¡Guardián del bosque!"
            2 -> "¡Bosque protegido!"
            else -> "¡Lo lograste, guardián!"
        }
        val summary = when {
            arcade.shieldsLostTotal == 0 && arcade.wavesFailed == 0 ->
                "Superaste las ${arcade.config.waves.size} oleadas sin que se colara ni un virus. ¡Reflejos de campeón!"
            arcade.wavesFailed == 0 ->
                "Superaste las ${arcade.config.waves.size} oleadas perdiendo solo ${arcade.shieldsLostTotal} escudo${if (arcade.shieldsLostTotal > 1) "s" else ""}. ¡Muy bien!"
            else ->
                "Los virus te ganaron ${arcade.wavesFailed} vez${if (arcade.wavesFailed > 1) "es" else ""}, pero no te rendiste y salvaste el bosque."
        }
        finishMinigame(challenge, arcade.stars, title, summary)
    }

    private fun finishChatGame(challenge: Challenge, chat: ChatGameState) {
        val title = when (chat.stars) {
            3 -> "¡Final perfecto!"
            2 -> "¡Gran final!"
            else -> "¡Lo lograste!"
        }
        val summary = when (chat.mistakes) {
            0 -> "Tomaste todas las decisiones correctas a la primera. ¡Eres un verdadero líder!"
            1 -> "Te equivocaste solo una vez, pero supiste corregir el rumbo. ¡Muy bien!"
            else -> "Te equivocaste ${chat.mistakes} veces, pero aprendiste de cada error y llegaste al mejor final."
        }
        finishMinigame(challenge, chat.stars, title, summary)
    }

    // ── Minijuego de deslizar ────────────────────────────────────────────────

    /** El niño deslizó la carta de arriba: [saidSafe] = true si la mandó al lado "seguro". */
    fun swipeCard(saidSafe: Boolean) {
        val state = _uiState.value
        val challenge = state.challenge ?: return
        val game = state.swipe ?: return
        if (game.awaitingAck) return
        val card = game.queue.firstOrNull() ?: return

        val correct = card.isSafe == saidSafe
        val rest = game.queue.drop(1)
        // Sin castigo: si falla, la carta vuelve al final del mazo para intentarlo otra vez
        val newQueue = if (correct) rest else rest + card
        val finished = newQueue.isEmpty()

        val newGame = game.copy(
            queue = newQueue,
            sorted = if (correct) game.sorted + 1 else game.sorted,
            mistakes = if (correct) game.mistakes else game.mistakes + 1,
            streak = if (correct) game.streak + 1 else 0,
            swipeCount = game.swipeCount + 1,
            lastResult = SwipeResult(card, correct, isFinal = finished),
            awaitingAck = !correct
        )
        _uiState.update { it.copy(swipe = newGame) }

        if (finished) finishSwipeGame(challenge, newGame)
    }

    fun acknowledgeSwipeMistake() {
        _uiState.update { state ->
            state.copy(swipe = state.swipe?.copy(awaitingAck = false))
        }
    }

    private fun finishSwipeGame(challenge: Challenge, game: SwipeGameState) {
        val title = when (game.stars) {
            3 -> "¡Clasificación perfecta!"
            2 -> "¡Muy bien, cazador!"
            else -> "¡Lo lograste!"
        }
        val summary = when (game.mistakes) {
            0 -> "Clasificaste las ${game.total} cartas sin un solo error. ¡Nadie te engaña!"
            1 -> "Clasificaste las ${game.total} cartas con solo 1 error. ¡Casi perfecto!"
            else -> "Clasificaste las ${game.total} cartas. Te equivocaste ${game.mistakes} veces, pero aprendiste de cada una."
        }
        finishMinigame(challenge, game.stars, title, summary)
    }

    // ── Minijuego del detective ──────────────────────────────────────────────

    /** El niño tocó la escena: [clueId] es la pista tocada, o null si tocó algo normal. */
    fun tapScene(clueId: String?) {
        val state = _uiState.value
        val challenge = state.challenge ?: return
        val game = state.spot ?: return
        if (game.foundIds.size == game.total) return
        // Volver a tocar una pista ya encontrada no cuenta como fallo
        if (clueId != null && clueId in game.foundIds) return

        val isClue = clueId != null && game.clues.any { it.id == clueId }
        val newGame = if (isClue) {
            val found = game.foundIds + clueId!!
            game.copy(
                foundIds = found,
                hintClueId = game.hintClueId?.takeIf { it != clueId },
                eventCount = game.eventCount + 1,
                lastEvent = SpotEvent(clueId, isFinal = found.size == game.total)
            )
        } else {
            game.copy(
                misses = game.misses + 1,
                eventCount = game.eventCount + 1,
                lastEvent = SpotEvent(null)
            )
        }
        _uiState.update { it.copy(spot = newGame) }

        if (newGame.foundIds.size == newGame.total) finishSpotGame(challenge, newGame)
    }

    /** Muestra la ayuda de la siguiente pista sin encontrar. */
    fun useSpotHint() {
        _uiState.update { state ->
            val game = state.spot ?: return@update state
            if (game.hintClueId != null) return@update state
            val next = game.clues.firstOrNull { it.id !in game.foundIds } ?: return@update state
            state.copy(spot = game.copy(hintClueId = next.id, hintsUsed = game.hintsUsed + 1))
        }
    }

    private fun finishSpotGame(challenge: Challenge, game: SpotGameState) {
        val title = when (game.stars) {
            3 -> "¡Detective experto!"
            2 -> "¡Caso resuelto!"
            else -> "¡Lo descubriste todo!"
        }
        val summary = when {
            game.misses == 0 && game.hintsUsed == 0 ->
                "Encontraste las ${game.total} pistas sin fallar ni pedir ayuda. ¡Tienes ojo de detective!"
            game.hintsUsed == 0 ->
                "Encontraste las ${game.total} pistas. Cada vez detectas las señales más rápido."
            else ->
                "Encontraste las ${game.total} pistas (usaste ${game.hintsUsed} ayuda${if (game.hintsUsed > 1) "s" else ""}). ¡La próxima las verás solo!"
        }
        finishMinigame(challenge, game.stars, title, summary)
    }

    /** Cierre común de los minijuegos: modal de victoria con estrellas y reto completado. */
    private fun finishMinigame(challenge: Challenge, stars: Int, title: String, summary: String) {
        val starText = "★".repeat(stars) + "☆".repeat(3 - stars)
        _uiState.update {
            it.copy(
                isCorrect = true,
                resultTitle = "$title $starText",
                feedbackText = summary,
                educationalTip = challenge.educationalTip,
                showFeedback = true
            )
        }
        viewModelScope.launch {
            repository.completeChallenge(moduleId, challenge.id, challenge.points)
            _uiState.update { it.copy(isChallengeCompleted = true) }
        }
    }

    fun selectOption(option: Option) {
        val challenge = _uiState.value.challenge ?: return
        val isCorrect = option.isCorrect

        _uiState.update {
            it.copy(
                selectedOption = option,
                isCorrect = isCorrect,
                feedbackText = option.feedback,
                educationalTip = challenge.educationalTip,
                showFeedback = true
            )
        }

        if (isCorrect) {
            viewModelScope.launch {
                repository.completeChallenge(moduleId, challenge.id, challenge.points)
                _uiState.update { it.copy(isChallengeCompleted = true) }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(showFeedback = false) }
    }

    companion object {
        fun provideFactory(
            repository: CyberRepository,
            moduleId: String,
            challengeId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameplayViewModel(repository, moduleId, challengeId) as T
            }
        }
    }
}
