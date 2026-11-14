package io.github.byckyka.cybertura.ui.viewmodel

import io.github.byckyka.cybertura.data.model.ChatChoice
import io.github.byckyka.cybertura.data.model.Option
import io.github.byckyka.cybertura.game.engine.ArcadeWaveSim
import io.github.byckyka.cybertura.game.engine.KeyStrength

// Reglas de los minijuegos: funciones puras que reciben un estado y devuelven el siguiente.
// No tocan la interfaz, el reloj ni el almacenamiento, así que se prueban sin Android.
// Devuelven null cuando la jugada no cuenta (por ejemplo, un toque con el juego bloqueado).

// ── Deslizar: ¿seguro o trampa? ──────────────────────────────────────────────

/** El niño clasificó la carta de arriba: [saidSafe] = true si la mandó al lado "seguro". */
fun SwipeGameState.afterSwipe(saidSafe: Boolean): SwipeGameState? {
    if (awaitingAck) return null
    val card = queue.firstOrNull() ?: return null
    val correct = card.isSafe == saidSafe
    val rest = queue.drop(1)
    // Sin castigo: si falla, la carta vuelve al final del mazo para intentarlo otra vez
    val newQueue = if (correct) rest else rest + card
    return copy(
        queue = newQueue,
        sorted = if (correct) sorted + 1 else sorted,
        mistakes = if (correct) mistakes else mistakes + 1,
        streak = if (correct) streak + 1 else 0,
        swipeCount = swipeCount + 1,
        lastResult = SwipeResult(card, correct, isFinal = newQueue.isEmpty()),
        awaitingAck = !correct
    )
}

/** El niño leyó la explicación de la carta que falló: el mazo se desbloquea. */
fun SwipeGameState.acknowledged(): SwipeGameState = copy(awaitingAck = false)

val SwipeGameState.isFinished: Boolean get() = queue.isEmpty()

// ── Detective: busca las pistas ──────────────────────────────────────────────

val SpotGameState.isFinished: Boolean get() = foundIds.size == total

/** El niño tocó la escena: [clueId] es la pista tocada, o null si tocó algo normal. */
fun SpotGameState.afterTap(clueId: String?): SpotGameState? {
    if (isFinished) return null
    // Volver a tocar una pista ya encontrada no cuenta como fallo
    if (clueId != null && clueId in foundIds) return null
    val isClue = clueId != null && clues.any { it.id == clueId }
    return if (isClue) {
        val found = foundIds + clueId!!
        copy(
            foundIds = found,
            hintClueId = hintClueId?.takeIf { it != clueId },
            eventCount = eventCount + 1,
            lastEvent = SpotEvent(clueId, isFinal = found.size == total)
        )
    } else {
        copy(misses = misses + 1, eventCount = eventCount + 1, lastEvent = SpotEvent(null))
    }
}

/** Muestra la ayuda de la siguiente pista sin encontrar; null si ya hay una ayuda visible o no queda nada. */
fun SpotGameState.withHint(): SpotGameState? {
    if (hintClueId != null) return null
    val next = clues.firstOrNull { it.id !in foundIds } ?: return null
    return copy(hintClueId = next.id, hintsUsed = hintsUsed + 1)
}

// ── Forja de llaves ──────────────────────────────────────────────────────────

/** Dureza mínima que exige la ronda actual (sin dato en el contenido se exige Hierro). */
val ForgeGameState.goalLevel: Int get() = (round.minLevel ?: 2).coerceIn(0, KeyStrength.MAX_LEVEL)

/** Se superó la última ronda: el minijuego terminó. */
val ForgeGameState.isFinished: Boolean get() = phase == ForgePhase.SUCCESS && isLastRound

/** Pone o quita un bloque de la llave. */
fun ForgeGameState.toggleBlock(blockId: String): ForgeGameState = when {
    phase != ForgePhase.EDITING -> this
    blockId in selectedIds -> copy(selectedIds = selectedIds - blockId)
    selectedIds.size >= ForgeGameState.MAX_BLOCKS -> this
    else -> copy(selectedIds = selectedIds + blockId)
}

/** El ladrón pone a prueba la llave armada. El resultado queda en `phase`: SUCCESS o FAIL. */
fun ForgeGameState.afterForge(): ForgeGameState? {
    if (phase != ForgePhase.EDITING || selectedIds.isEmpty()) return null
    val key = key
    val level = KeyStrength.level(key)
    val goal = goalLevel
    val trap = selectedBlocks.firstOrNull { it.isTrap }
    return when {
        trap != null -> copy(
            phase = ForgePhase.FAIL,
            mistakes = mistakes + 1,
            // Quitamos el bloque trampa para que el niño vea qué falló y pueda reintentar
            selectedIds = selectedIds - trap.id,
            resultTitle = "💥 ¡El ladrón la adivinó!",
            resultMessage = (trap.feedback ?: "Ese dato es muy fácil de adivinar.") +
                " Quitamos \"${trap.text}\" de tu llave.",
            eventCount = eventCount + 1
        )
        level < goal -> {
            val material = KeyStrength.MATERIALS[level]
            val needed = KeyStrength.MATERIALS[goal]
            copy(
                phase = ForgePhase.FAIL,
                mistakes = mistakes + 1,
                resultTitle = "💥 ¡La llave se rompió!",
                resultMessage = "Tu llave es de ${material.emoji} ${material.name} y el ladrón la forzó. " +
                    "Necesitas al menos ${needed.emoji} ${needed.name}. " +
                    (KeyStrength.tips(key).firstOrNull() ?: ""),
                eventCount = eventCount + 1
            )
        }
        else -> {
            val material = KeyStrength.MATERIALS[level]
            copy(
                phase = ForgePhase.SUCCESS,
                forgedKeys = forgedKeys + key,
                resultTitle = "🛡️ ¡La llave resistió!",
                resultMessage = "Forjaste una llave de ${material.emoji} ${material.name}. " +
                    "El ladrón lo intentó con todas sus fuerzas… ¡y no pudo abrirla!",
                eventCount = eventCount + 1
            )
        }
    }
}

/** El niño decide qué hacer con su llave ante el ladrón. */
fun ForgeGameState.afterOption(optionId: String): ForgeGameState? {
    if (phase != ForgePhase.DEFENDING || optionId in disabledOptionIds) return null
    val option = round.options?.firstOrNull { it.id == optionId } ?: return null
    return if (option.isGood) {
        copy(
            phase = ForgePhase.SUCCESS,
            resultTitle = "🛡️ ¡Llave protegida!",
            resultMessage = option.feedback,
            eventCount = eventCount + 1
        )
    } else {
        copy(
            phase = ForgePhase.FAIL,
            mistakes = mistakes + 1,
            disabledOptionIds = disabledOptionIds + option.id,
            resultTitle = "🦹 ¡El ladrón se llevó tu llave!",
            resultMessage = option.feedback,
            eventCount = eventCount + 1
        )
    }
}

/**
 * Botón del panel de resultado: tras un éxito pasa a la siguiente ronda y tras un fallo deja
 * reintentar la misma. Null si no hay nada que continuar (o si ya se superó la última ronda).
 */
fun ForgeGameState.afterContinue(): ForgeGameState? = when (phase) {
    ForgePhase.SUCCESS -> if (isLastRound) null else copy(
        roundIndex = roundIndex + 1,
        selectedIds = emptyList(),
        disabledOptionIds = emptySet(),
        phase = ForgeGameState.phaseFor(rounds[roundIndex + 1]),
        resultTitle = null,
        resultMessage = null
    )
    ForgePhase.FAIL -> copy(phase = ForgeGameState.phaseFor(round), resultTitle = null, resultMessage = null)
    else -> null
}

// ── Chat de grupo ────────────────────────────────────────────────────────────

/** La opción [choiceId] del paso actual, si se puede elegir ahora mismo. */
fun ChatGameState.selectableChoice(choiceId: String): ChatChoice? {
    if (phase != ChatPhase.CHOOSING || choiceId in disabledChoiceIds) return null
    return currentStep?.choices?.firstOrNull { it.id == choiceId }
}

/** El jugador respondió: el medidor sube o baja y la conversación sigue. */
fun ChatGameState.afterChoice(choice: ChatChoice): ChatGameState = copy(
    phase = ChatPhase.PLAYING,
    meter = (meter + if (choice.isGood) 1 else -1).coerceIn(0, meterIcons.size - 1),
    mistakes = if (choice.isGood) mistakes else mistakes + 1
)

/** La respuesta no ayudó: se explica por qué y esa opción queda descartada. */
fun ChatGameState.awaitingRetry(choice: ChatChoice, badLineId: Int): ChatGameState = copy(
    phase = ChatPhase.RETRY,
    retryFeedback = choice.feedback,
    lastBadLineId = badLineId,
    disabledChoiceIds = disabledChoiceIds + choice.id
)

/** Tras leer la explicación, el mensaje equivocado se "deshace" y se vuelve a elegir. */
fun ChatGameState.afterRetry(): ChatGameState? {
    if (phase != ChatPhase.RETRY) return null
    return copy(
        lines = lines.map { if (it.id == lastBadLineId) it.copy(retracted = true) else it },
        phase = ChatPhase.CHOOSING,
        retryFeedback = null,
        lastBadLineId = null
    )
}

// ── Arcade: defiende el bosque ───────────────────────────────────────────────

/** Empieza (o repite) la oleada actual con la simulación [sim]; null si ya se está jugando o terminó. */
fun ArcadeGameState.started(sim: ArcadeWaveSim): ArcadeGameState? {
    if (phase == ArcadePhase.PLAYING || finished) return null
    return copy(phase = ArcadePhase.PLAYING, attempt = attempt + 1, sim = sim)
}

/** La oleada en curso terminó: se repite si se perdió, o se pasa a la siguiente (o al final). */
fun ArcadeGameState.afterWave(): ArcadeGameState? {
    if (phase != ArcadePhase.PLAYING) return null
    val ended = sim?.takeIf { it.isOver } ?: return null
    val base = copy(shieldsLostTotal = shieldsLostTotal + ended.shieldsLost, sim = null)
    return when {
        ended.failed -> base.copy(phase = ArcadePhase.WAVE_FAILED, wavesFailed = wavesFailed + 1)
        waveIndex == config.waves.lastIndex ->
            base.copy(phase = ArcadePhase.WAVE_CLEAR, lastTip = wave.tip, finished = true)
        else -> base.copy(phase = ArcadePhase.WAVE_CLEAR, waveIndex = waveIndex + 1, lastTip = wave.tip)
    }
}

// ── Pregunta de situación real ───────────────────────────────────────────────

/** La opción se puede elegir: no hay un resultado a la vista, el reto sigue abierto y no está descartada. */
fun GameplayUiState.canSelect(option: Option): Boolean =
    !showFeedback && !isChallengeCompleted && option.id !in discardedOptionIds

/** El niño eligió una opción: una equivocada queda tachada y se cuenta; la correcta cierra la pregunta. */
fun GameplayUiState.afterOption(option: Option, educationalTip: String): GameplayUiState = copy(
    selectedOption = option,
    isCorrect = option.isCorrect,
    feedbackText = option.feedback,
    educationalTip = educationalTip,
    showFeedback = true,
    wrongAnswers = if (option.isCorrect) wrongAnswers else wrongAnswers + 1,
    discardedOptionIds = if (option.isCorrect) discardedOptionIds else discardedOptionIds + option.id
)
