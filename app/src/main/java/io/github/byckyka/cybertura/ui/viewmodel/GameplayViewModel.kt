package io.github.byckyka.cybertura.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.byckyka.cybertura.data.model.ArcadeItem
import io.github.byckyka.cybertura.data.model.Challenge
import io.github.byckyka.cybertura.data.model.ChatMessage
import io.github.byckyka.cybertura.data.model.CyberModule
import io.github.byckyka.cybertura.data.model.Option
import io.github.byckyka.cybertura.data.repository.CyberRepository
import io.github.byckyka.cybertura.game.engine.ArcadeWaveSim
import io.github.byckyka.cybertura.research.ResearchLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameplayUiState(
    val module: CyberModule? = null,
    val challenge: Challenge? = null,
    val challengeIndex: Int = 0,
    val totalChallenges: Int = 0,
    val selectedOption: Option? = null,
    /** Opciones de la pregunta, ya barajadas para que la correcta no esté siempre en el mismo lugar. */
    val options: List<Option> = emptyList(),
    /** Opciones que ya se eligieron y no eran la correcta: quedan tachadas. */
    val discardedOptionIds: Set<String> = emptySet(),
    val wrongAnswers: Int = 0,
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

/**
 * Conduce un reto: carga su contenido, aplica las jugadas, registra el intento para el piloto y
 * guarda el progreso al terminar.
 *
 * Las reglas de cada minijuego están en MinigameRules.kt como funciones puras; aquí solo queda
 * lo que depende del tiempo (el chat que "escribe"), del registro y del almacenamiento.
 */
class GameplayViewModel(
    private val repository: CyberRepository,
    private val moduleId: String,
    private val challengeId: String,
    private val researchLogger: ResearchLogger? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameplayUiState())
    val uiState: StateFlow<GameplayUiState> = _uiState.asStateFlow()

    /** Intento que se está registrando para el piloto; null si el modo investigador no está activo. */
    private var attempt: ResearchLogger.Attempt? = null

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
                    options = challenge.options.orEmpty().shuffled(),
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
            attempt = researchLogger?.startAttempt(moduleId, module.title, challenge.id, challenge.type)
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
        val choice = chat.selectableChoice(choiceId) ?: return
        attempt?.answer(choice.isGood)

        updateChat { it.afterChoice(choice) }
        val lineId = appendLine(ChatLineKind.PLAYER, choice.text)

        viewModelScope.launch {
            delay(400)
            revealMessages(choice.reactions)
            if (choice.isGood) {
                appendLine(ChatLineKind.GOOD_TIP, choice.feedback)
                delay(1200)
                playChatStep(chat.stepIndex + 1)
            } else {
                updateChat { it.awaitingRetry(choice, lineId) }
            }
        }
    }

    /** Tras leer la explicación de una mala decisión, el jugador vuelve a elegir. */
    fun retryChatReply() {
        val next = _uiState.value.chat?.afterRetry() ?: return
        updateChat { next }
        appendLine(ChatLineKind.NOTE, "⏪ Volvamos a intentarlo")
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
        finishMinigame(challenge, chat.stars, title, summary, errors = chat.mistakes)
    }

    // ── Minijuego de la forja de llaves ──────────────────────────────────────

    private fun setForge(forge: ForgeGameState) {
        _uiState.update { it.copy(forge = forge) }
    }

    /** Pone o quita un bloque de la llave. */
    fun toggleForgeBlock(blockId: String) {
        val forge = _uiState.value.forge ?: return
        setForge(forge.toggleBlock(blockId))
    }

    /** El ladrón pone a prueba la llave armada. */
    fun forgeKey() {
        val next = _uiState.value.forge?.afterForge() ?: return
        attempt?.answer(next.phase == ForgePhase.SUCCESS)
        setForge(next)
    }

    /** El niño decide qué hacer con su llave ante el ladrón. */
    fun chooseForgeOption(optionId: String) {
        val next = _uiState.value.forge?.afterOption(optionId) ?: return
        attempt?.answer(next.phase == ForgePhase.SUCCESS)
        setForge(next)
    }

    /** Botón del panel de resultado: siguiente ronda (tras un éxito) o reintentar (tras un fallo). */
    fun continueForge() {
        val forge = _uiState.value.forge ?: return
        if (forge.isFinished) {
            val challenge = _uiState.value.challenge ?: return
            finishForgeGame(challenge, forge)
        } else {
            forge.afterContinue()?.let { setForge(it) }
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
        finishMinigame(challenge, forge.stars, title, summary, errors = forge.mistakes)
    }

    // ── Arcade "Defiende el bosque" ──────────────────────────────────────────

    /** Empieza (o repite) la oleada actual. */
    fun startArcadeWave() {
        val arcade = _uiState.value.arcade ?: return
        val next = arcade.started(ArcadeWaveSim(arcade.wave, arcade.config.items, arcade.shields)) ?: return
        _uiState.update { it.copy(arcade = next) }
    }

    /** La pantalla avisa que la oleada en curso terminó; el resultado se lee de la simulación. */
    fun onArcadeWaveEnded() {
        val state = _uiState.value
        val next = state.arcade?.afterWave() ?: return
        _uiState.update { it.copy(arcade = next) }
        if (next.finished) {
            val challenge = state.challenge ?: return
            finishArcadeGame(challenge, next)
        }
    }

    /** Un objeto del arcade se resolvió: el niño lo tocó o lo dejó llegar al bosque. */
    fun onArcadeItemResolved(item: ArcadeItem, tapped: Boolean) {
        attempt?.answer(tapped == item.isThreat)
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
        finishMinigame(challenge, arcade.stars, title, summary, errors = arcade.shieldsLostTotal)
    }

    // ── Minijuego de deslizar ────────────────────────────────────────────────

    /** El niño deslizó la carta de arriba: [saidSafe] = true si la mandó al lado "seguro". */
    fun swipeCard(saidSafe: Boolean) {
        val state = _uiState.value
        val challenge = state.challenge ?: return
        val next = state.swipe?.afterSwipe(saidSafe) ?: return
        attempt?.answer(next.lastResult?.wasCorrect == true)
        _uiState.update { it.copy(swipe = next) }
        if (next.isFinished) finishSwipeGame(challenge, next)
    }

    fun acknowledgeSwipeMistake() {
        _uiState.update { state -> state.copy(swipe = state.swipe?.acknowledged()) }
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
        finishMinigame(challenge, game.stars, title, summary, errors = game.mistakes)
    }

    // ── Minijuego del detective ──────────────────────────────────────────────

    /** El niño tocó la escena: [clueId] es la pista tocada, o null si tocó algo normal. */
    fun tapScene(clueId: String?) {
        val state = _uiState.value
        val challenge = state.challenge ?: return
        val next = state.spot?.afterTap(clueId) ?: return
        attempt?.answer(next.lastEvent?.clueId != null)
        _uiState.update { it.copy(spot = next) }
        if (next.isFinished) finishSpotGame(challenge, next)
    }

    /** Muestra la ayuda de la siguiente pista sin encontrar. */
    fun useSpotHint() {
        val next = _uiState.value.spot?.withHint() ?: return
        _uiState.update { it.copy(spot = next) }
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
        finishMinigame(challenge, game.stars, title, summary, errors = game.misses, hints = game.hintsUsed)
    }

    // ── Cierre común ─────────────────────────────────────────────────────────

    /** Cierre común de los minijuegos: modal de victoria con estrellas y reto completado. */
    private fun finishMinigame(
        challenge: Challenge,
        stars: Int,
        title: String,
        summary: String,
        errors: Int,
        hints: Int = 0
    ) {
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
        // Las estrellas se guardan como mejor marca: repasar un reto puede subirlas
        saveCompletion(challenge, errors = errors, hints = hints, stars = stars)
    }

    /** Cierra el intento del piloto y guarda el reto como completado. */
    private fun saveCompletion(challenge: Challenge, errors: Int, hints: Int, stars: Int?) {
        viewModelScope.launch {
            // La XP solo se gana la primera vez que se completa el reto
            val firstTime = challenge.id !in repository.userProgress.first().completedChallengeIds
            attempt?.finish(errors, hints, stars ?: 0, xpGained = if (firstTime) challenge.points else 0)
            attempt = null
            repository.completeChallenge(moduleId, challenge.id, challenge.points, stars)
            _uiState.update { it.copy(isChallengeCompleted = true) }
        }
    }

    // ── Pregunta de situación real ───────────────────────────────────────────

    /**
     * El niño eligió una opción. Si no es la correcta se explica por qué, queda tachada y
     * puede volver a elegir: equivocarse no quita puntos, pero sí queda en el registro del piloto.
     */
    fun selectOption(option: Option) {
        val state = _uiState.value
        val challenge = state.challenge ?: return
        if (!state.canSelect(option)) return
        attempt?.answer(option.isCorrect)

        val next = state.afterOption(option, challenge.educationalTip)
        _uiState.value = next
        // La pregunta no da estrellas
        if (option.isCorrect) saveCompletion(challenge, errors = next.wrongAnswers, hints = 0, stars = null)
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(showFeedback = false) }
    }

    override fun onCleared() {
        // Salió del reto sin terminarlo: queda registrado como abandonado
        val state = _uiState.value
        attempt?.abandon(
            errors = state.swipe?.mistakes ?: state.spot?.misses ?: state.chat?.mistakes
                ?: state.forge?.mistakes ?: state.arcade?.shieldsLostTotal ?: state.wrongAnswers,
            hints = state.spot?.hintsUsed ?: 0
        )
        attempt = null
    }

    companion object {
        fun provideFactory(
            repository: CyberRepository,
            moduleId: String,
            challengeId: String,
            researchLogger: ResearchLogger? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameplayViewModel(repository, moduleId, challengeId, researchLogger) as T
            }
        }
    }
}
