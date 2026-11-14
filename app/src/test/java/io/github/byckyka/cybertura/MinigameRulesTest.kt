package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.data.model.ArcadeChallenge
import io.github.byckyka.cybertura.data.model.ArcadeItem
import io.github.byckyka.cybertura.data.model.ArcadeWave
import io.github.byckyka.cybertura.data.model.ChatChoice
import io.github.byckyka.cybertura.data.model.ChatStep
import io.github.byckyka.cybertura.data.model.ChatStory
import io.github.byckyka.cybertura.data.model.Clue
import io.github.byckyka.cybertura.data.model.ForgeBlock
import io.github.byckyka.cybertura.data.model.ForgeOption
import io.github.byckyka.cybertura.data.model.ForgeRound
import io.github.byckyka.cybertura.data.model.Option
import io.github.byckyka.cybertura.data.model.SwipeCard
import io.github.byckyka.cybertura.game.engine.ArcadeWaveSim
import io.github.byckyka.cybertura.ui.viewmodel.ArcadeGameState
import io.github.byckyka.cybertura.ui.viewmodel.ArcadePhase
import io.github.byckyka.cybertura.ui.viewmodel.ChatGameState
import io.github.byckyka.cybertura.ui.viewmodel.ChatLine
import io.github.byckyka.cybertura.ui.viewmodel.ChatLineKind
import io.github.byckyka.cybertura.ui.viewmodel.ChatPhase
import io.github.byckyka.cybertura.ui.viewmodel.ForgeGameState
import io.github.byckyka.cybertura.ui.viewmodel.ForgePhase
import io.github.byckyka.cybertura.ui.viewmodel.GameplayUiState
import io.github.byckyka.cybertura.ui.viewmodel.SpotGameState
import io.github.byckyka.cybertura.ui.viewmodel.SwipeGameState
import io.github.byckyka.cybertura.ui.viewmodel.acknowledged
import io.github.byckyka.cybertura.ui.viewmodel.afterChoice
import io.github.byckyka.cybertura.ui.viewmodel.afterContinue
import io.github.byckyka.cybertura.ui.viewmodel.afterForge
import io.github.byckyka.cybertura.ui.viewmodel.afterOption
import io.github.byckyka.cybertura.ui.viewmodel.afterRetry
import io.github.byckyka.cybertura.ui.viewmodel.afterSwipe
import io.github.byckyka.cybertura.ui.viewmodel.afterTap
import io.github.byckyka.cybertura.ui.viewmodel.afterWave
import io.github.byckyka.cybertura.ui.viewmodel.awaitingRetry
import io.github.byckyka.cybertura.ui.viewmodel.canSelect
import io.github.byckyka.cybertura.ui.viewmodel.isFinished
import io.github.byckyka.cybertura.ui.viewmodel.selectableChoice
import io.github.byckyka.cybertura.ui.viewmodel.started
import io.github.byckyka.cybertura.ui.viewmodel.toggleBlock
import io.github.byckyka.cybertura.ui.viewmodel.withHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas de los minijuegos: qué cuenta como acierto o error y cuándo se termina. */
class MinigameRulesTest {

    // ── Deslizar ─────────────────────────────────────────────────────────────

    private fun card(id: String, safe: Boolean) = SwipeCard(id = id, text = id, isSafe = safe, feedback = "porque sí")

    private fun swipeGame() = listOf(card("mama", true), card("premio", false)).let { SwipeGameState(queue = it, total = it.size) }

    @Test
    fun deslizar_bien_avanza_y_suma_racha() {
        val next = swipeGame().afterSwipe(saidSafe = true)!!
        assertEquals(1, next.sorted)
        assertEquals(0, next.mistakes)
        assertEquals(1, next.streak)
        assertEquals(listOf("premio"), next.queue.map { it.id })
        assertFalse(next.awaitingAck)
        assertTrue(next.lastResult!!.wasCorrect)
    }

    @Test
    fun deslizar_mal_devuelve_la_carta_al_final_y_bloquea_hasta_leer() {
        val next = swipeGame().afterSwipe(saidSafe = false)!!
        assertEquals(1, next.mistakes)
        assertEquals(0, next.streak)
        assertEquals(listOf("premio", "mama"), next.queue.map { it.id })
        assertTrue(next.awaitingAck)
        // Bloqueado: otro deslizamiento no cuenta hasta leer la explicación
        assertNull(next.afterSwipe(saidSafe = false))
        assertNotNull(next.acknowledged().afterSwipe(saidSafe = false))
    }

    @Test
    fun deslizar_termina_cuando_no_quedan_cartas() {
        val done = swipeGame().afterSwipe(true)!!.afterSwipe(false)!!
        assertTrue(done.isFinished)
        assertTrue(done.lastResult!!.isFinal)
        assertEquals(3, done.stars)
        assertNull(done.afterSwipe(true))
    }

    @Test
    fun las_estrellas_de_deslizar_bajan_con_los_errores() {
        fun withMistakes(n: Int) = swipeGame().copy(mistakes = n).stars
        assertEquals(listOf(3, 2, 2, 1), listOf(0, 1, 2, 3).map(::withMistakes))
    }

    // ── Detective ────────────────────────────────────────────────────────────

    private fun spotGame() = SpotGameState(
        clues = listOf(Clue("prisa", "Prisa", "Te apuran"), Clue("link", "Enlace", "Enlace raro"))
    )

    @Test
    fun tocar_una_pista_la_encuentra_y_tocar_otra_cosa_cuenta_como_fallo() {
        val found = spotGame().afterTap("prisa")!!
        assertEquals(listOf("prisa"), found.foundIds)
        assertEquals(0, found.misses)
        val missed = found.afterTap(null)!!
        assertEquals(1, missed.misses)
        assertNull(missed.lastEvent!!.clueId)
        // Un id que no es pista también es un fallo
        assertEquals(2, missed.afterTap("inventada")!!.misses)
    }

    @Test
    fun volver_a_tocar_una_pista_encontrada_no_cuenta() {
        val found = spotGame().afterTap("prisa")!!
        assertNull(found.afterTap("prisa"))
    }

    @Test
    fun detective_termina_al_encontrar_todas_las_pistas() {
        val done = spotGame().afterTap("prisa")!!.afterTap("link")!!
        assertTrue(done.isFinished)
        assertTrue(done.lastEvent!!.isFinal)
        assertNull(done.afterTap(null))
    }

    @Test
    fun la_ayuda_apunta_a_la_siguiente_pista_y_se_apaga_al_encontrarla() {
        val hinted = spotGame().withHint()!!
        assertEquals("prisa", hinted.hintClueId)
        assertEquals(1, hinted.hintsUsed)
        // Con una ayuda visible no se puede pedir otra
        assertNull(hinted.withHint())
        val found = hinted.afterTap("prisa")!!
        assertNull(found.hintClueId)
        assertEquals("link", found.withHint()!!.hintClueId)
        // Cada ayuda pesa como 2 fallos en las estrellas
        assertEquals(2, spotGame().copy(hintsUsed = 1).stars)
    }

    // ── Forja de llaves ──────────────────────────────────────────────────────

    private val buildRound = ForgeRound(
        id = "construir",
        minLevel = 2,
        blocks = listOf(
            ForgeBlock("gato", "GatoAzul"),
            ForgeBlock("salta", "Salta"),
            ForgeBlock("num", "2026"),
            ForgeBlock("sim", "!"),
            ForgeBlock("nombre", "Sofia", isTrap = true, feedback = "Es tu nombre.")
        )
    )
    private val defendRound = ForgeRound(
        id = "defender",
        options = listOf(
            ForgeOption("dar", "Se la doy", isGood = false, feedback = "No se comparte."),
            ForgeOption("no", "No se la doy", isGood = true, feedback = "Bien.")
        )
    )

    private fun forgeGame() = ForgeGameState(rounds = listOf(buildRound, defendRound), phase = ForgePhase.EDITING)

    private fun ForgeGameState.with(vararg ids: String) = ids.fold(this) { state, id -> state.toggleBlock(id) }

    @Test
    fun los_bloques_se_ponen_y_se_quitan() {
        val game = forgeGame().with("gato", "num")
        assertEquals("GatoAzul2026", game.key)
        assertEquals("2026", game.toggleBlock("gato").key)
    }

    @Test
    fun una_llave_vacia_no_se_puede_forjar() {
        assertNull(forgeGame().afterForge())
    }

    @Test
    fun una_llave_debil_se_rompe_y_deja_reintentar() {
        val failed = forgeGame().with("gato").afterForge()!!
        assertEquals(ForgePhase.FAIL, failed.phase)
        assertEquals(1, failed.mistakes)
        assertEquals(ForgePhase.EDITING, failed.afterContinue()!!.phase)
    }

    @Test
    fun un_bloque_trampa_falla_aunque_la_llave_sea_larga_y_se_quita() {
        val failed = forgeGame().with("gato", "salta", "num", "sim", "nombre").afterForge()!!
        assertEquals(ForgePhase.FAIL, failed.phase)
        assertFalse("nombre" in failed.selectedIds)
        assertTrue(failed.resultMessage!!.contains("Es tu nombre."))
    }

    @Test
    fun una_llave_fuerte_resiste_y_pasa_a_la_ronda_de_defensa() {
        val ok = forgeGame().with("gato", "salta", "num", "sim").afterForge()!!
        assertEquals(ForgePhase.SUCCESS, ok.phase)
        assertEquals(listOf("GatoAzulSalta2026!"), ok.forgedKeys)
        assertFalse(ok.isFinished)
        val defend = ok.afterContinue()!!
        assertEquals(ForgePhase.DEFENDING, defend.phase)
        assertEquals(1, defend.roundIndex)
        assertTrue(defend.selectedIds.isEmpty())
    }

    @Test
    fun en_la_defensa_una_mala_decision_se_descarta_y_la_buena_termina_la_forja() {
        val defend = forgeGame().with("gato", "salta", "num", "sim").afterForge()!!.afterContinue()!!
        val failed = defend.afterOption("dar")!!
        assertEquals(ForgePhase.FAIL, failed.phase)
        val retry = failed.afterContinue()!!
        assertEquals(ForgePhase.DEFENDING, retry.phase)
        assertNull("La opción descartada no se puede volver a elegir", retry.afterOption("dar"))
        val won = retry.afterOption("no")!!
        assertTrue(won.isFinished)
        assertNull(won.afterContinue())
        assertEquals(2, won.stars)
    }

    @Test
    fun la_llave_admite_como_maximo_ocho_bloques() {
        val blocks = (1..10).map { ForgeBlock("b$it", "x$it") }
        val game = ForgeGameState(rounds = listOf(buildRound.copy(blocks = blocks)), phase = ForgePhase.EDITING)
        val full = blocks.fold(game) { state, block -> state.toggleBlock(block.id) }
        assertEquals(ForgeGameState.MAX_BLOCKS, full.selectedIds.size)
    }

    // ── Chat de grupo ────────────────────────────────────────────────────────

    private val good = ChatChoice("bien", "Paren, por favor", isGood = true, feedback = "Ayuda.")
    private val bad = ChatChoice("mal", "Jajaja", isGood = false, feedback = "No ayuda.")

    private fun chatGame() = ChatGameState(
        story = ChatStory(steps = listOf(ChatStep(choices = listOf(good, bad)), ChatStep())),
        phase = ChatPhase.CHOOSING,
        meter = 1
    )

    @Test
    fun una_buena_respuesta_sube_el_medidor_y_una_mala_lo_baja() {
        assertEquals(2, chatGame().afterChoice(good).meter)
        val worse = chatGame().afterChoice(bad)
        assertEquals(0, worse.meter)
        assertEquals(1, worse.mistakes)
        // El medidor no se sale de sus caritas
        assertEquals(0, worse.afterChoice(bad).meter)
        assertEquals(4, chatGame().copy(meter = 4).afterChoice(good).meter)
    }

    @Test
    fun solo_se_puede_responder_cuando_toca_y_con_opciones_no_descartadas() {
        assertEquals(good, chatGame().selectableChoice("bien"))
        assertNull(chatGame().copy(phase = ChatPhase.PLAYING).selectableChoice("bien"))
        assertNull(chatGame().selectableChoice("no-existe"))
        assertNull(chatGame().copy(disabledChoiceIds = setOf("mal")).selectableChoice("mal"))
    }

    @Test
    fun tras_una_mala_respuesta_se_explica_se_deshace_el_mensaje_y_se_vuelve_a_elegir() {
        val line = ChatLine(id = 0, kind = ChatLineKind.PLAYER, text = bad.text)
        val waiting = chatGame().afterChoice(bad).copy(lines = listOf(line)).awaitingRetry(bad, badLineId = 0)
        assertEquals(ChatPhase.RETRY, waiting.phase)
        assertEquals("No ayuda.", waiting.retryFeedback)
        assertTrue("mal" in waiting.disabledChoiceIds)

        val again = waiting.afterRetry()!!
        assertEquals(ChatPhase.CHOOSING, again.phase)
        assertTrue(again.lines.single().retracted)
        assertNull(again.selectableChoice("mal"))
        assertNotNull(again.selectableChoice("bien"))
        // Fuera de ese momento, "elegir otra vez" no hace nada
        assertNull(chatGame().afterRetry())
    }

    // ── Arcade ───────────────────────────────────────────────────────────────

    private val threat = ArcadeItem("virus", "🦠", "virus", isThreat = true, feedback = "")
    private val safe = ArcadeItem("tarea", "📄", "tarea", isThreat = false, feedback = "")

    private fun arcadeGame(waves: Int = 2) = ArcadeGameState(
        config = ArcadeChallenge(
            shields = 2,
            items = listOf(threat, safe),
            waves = List(waves) { ArcadeWave(count = 3, fallMs = 100, spawnMs = 50, tip = "consejo $it") }
        )
    )

    private fun ArcadeGameState.play(pool: List<ArcadeItem>): ArcadeGameState {
        val sim = ArcadeWaveSim(wave, pool, shields)
        val playing = started(sim)!!
        while (!sim.isOver) sim.advance(16)
        return playing.afterWave()!!
    }

    @Test
    fun una_oleada_superada_pasa_a_la_siguiente_con_su_consejo() {
        val next = arcadeGame().play(listOf(safe))
        assertEquals(ArcadePhase.WAVE_CLEAR, next.phase)
        assertEquals(1, next.waveIndex)
        assertEquals("consejo 0", next.lastTip)
        assertFalse(next.finished)
        assertNull(next.sim)
    }

    @Test
    fun una_oleada_perdida_se_repite_y_cuenta_todos_sus_escudos() {
        val failed = arcadeGame().play(listOf(threat))
        assertEquals(ArcadePhase.WAVE_FAILED, failed.phase)
        assertEquals(0, failed.waveIndex)
        assertEquals(1, failed.wavesFailed)
        assertEquals(2, failed.shieldsLostTotal)
        // Se puede volver a empezar la misma oleada
        assertEquals(2, failed.started(ArcadeWaveSim(failed.wave, listOf(safe), failed.shields))!!.attempt)
    }

    @Test
    fun superar_la_ultima_oleada_termina_el_arcade() {
        val done = arcadeGame(waves = 1).play(listOf(safe))
        assertTrue(done.finished)
        assertEquals(3, done.stars)
        assertNull(done.started(ArcadeWaveSim(done.wave, listOf(safe), done.shields)))
    }

    @Test
    fun una_oleada_que_no_ha_terminado_no_se_puede_cerrar() {
        val game = arcadeGame()
        val playing = game.started(ArcadeWaveSim(game.wave, listOf(safe), game.shields))!!
        assertNull(playing.afterWave())
        assertNull("No se puede empezar otra oleada encima", playing.started(ArcadeWaveSim(game.wave, listOf(safe), 2)))
    }

    // ── Pregunta de situación real ───────────────────────────────────────────

    private val right = Option("a", "Aviso a un adulto", isCorrect = true, feedback = "Bien.")
    private val wrong = Option("b", "Hago clic", isCorrect = false, feedback = "Era trampa.")

    @Test
    fun una_opcion_equivocada_queda_tachada_y_se_cuenta() {
        val state = GameplayUiState(options = listOf(right, wrong)).afterOption(wrong, "consejo")
        assertFalse(state.isCorrect)
        assertEquals(1, state.wrongAnswers)
        assertEquals(setOf("b"), state.discardedOptionIds)
        assertEquals("Era trampa.", state.feedbackText)
        // Con la explicación a la vista no se puede elegir; al cerrarla, la tachada sigue bloqueada
        assertFalse(state.canSelect(right))
        val closed = state.copy(showFeedback = false)
        assertFalse(closed.canSelect(wrong))
        assertTrue(closed.canSelect(right))
    }

    @Test
    fun acertar_a_la_primera_deja_cero_errores() {
        val state = GameplayUiState(options = listOf(right, wrong)).afterOption(right, "consejo")
        assertTrue(state.isCorrect)
        assertEquals(0, state.wrongAnswers)
        assertTrue(state.discardedOptionIds.isEmpty())
        assertEquals("consejo", state.educationalTip)
    }

    @Test
    fun una_pregunta_ya_completada_no_admite_mas_respuestas() {
        assertFalse(GameplayUiState(isChallengeCompleted = true).canSelect(right))
    }
}
