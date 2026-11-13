package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.data.model.Challenge
import io.github.byckyka.cybertura.game.engine.NpcRegistry
import io.github.byckyka.cybertura.research.AttemptRow
import io.github.byckyka.cybertura.research.ForegroundClock
import io.github.byckyka.cybertura.research.ResearchCsv
import io.github.byckyka.cybertura.research.ResearchReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los datos del piloto: cómo se mide el tiempo y cómo se arma el CSV que se exporta. */
class ResearchReportTest {

    private val firstMaster = NpcRegistry.questNpcs.first()
    private val moduleId = firstMaster.moduleId!!

    private fun attempt(
        session: String = "P01-20261113-0900",
        module: String = moduleId,
        type: String = Challenge.TYPE_CHAT_STORY,
        seconds: Int = 10,
        completed: Boolean = true,
        hits: Int = 3,
        errors: Int = 0,
        hints: Int = 0,
        stars: Int = 3,
        xp: Int = 100,
        title: String = "Módulo de prueba",
        challenge: String = "reto_1"
    ) = AttemptRow(
        sessionId = session, participant = session.substringBefore('-'), sessionStartMs = 1_800_000_000_000L,
        moduleId = module, moduleTitle = title, minigameType = type, durationMs = seconds * 1000L,
        completed = completed, hits = hits, errors = errors, hints = hints, stars = stars, xp = xp,
        challengeId = challenge
    )

    /** Filas de datos del informe (sin encabezado ni guía), ya separadas en columnas. */
    private fun dataRows(rows: List<AttemptRow>): List<List<String>> =
        ResearchReport.build(rows)
            .removePrefix("﻿")
            .split("\r\n")
            .drop(1)
            .takeWhile { it.isNotBlank() }
            .map { ResearchCsv.parseLine(it) }

    private fun List<String>.col(name: String) = this[ResearchReport.HEADER.indexOf(name)]

    // ── Reloj ────────────────────────────────────────────────────────────────

    @Test
    fun el_tiempo_en_segundo_plano_no_se_cuenta() {
        var now = 1_000L
        val clock = ForegroundClock { now }
        val start = clock.activeMs()

        now += 20_000          // 20 s jugando
        clock.onBackground()
        now += 300_000         // 5 min con la pantalla apagada
        clock.onForeground()
        now += 10_000          // 10 s más jugando

        assertEquals(30_000L, clock.activeMs() - start)
    }

    @Test
    fun el_reloj_no_avanza_mientras_la_app_esta_oculta() {
        var now = 0L
        val clock = ForegroundClock { now }
        val start = clock.activeMs()
        now += 5_000
        clock.onBackground()
        now += 60_000
        // Un intento que se cierra con la app oculta solo cuenta lo jugado
        assertEquals(5_000L, clock.activeMs() - start)
    }

    @Test
    fun avisos_repetidos_del_ciclo_de_vida_no_alteran_el_reloj() {
        var now = 0L
        val clock = ForegroundClock { now }
        val start = clock.activeMs()
        clock.onForeground()   // primer onStart: la app no venía de estar oculta
        now += 1_000
        clock.onBackground()
        now += 1_000
        clock.onBackground()   // dos onStop seguidos
        now += 1_000
        clock.onForeground()
        now += 1_000
        assertEquals(2_000L, clock.activeMs() - start)
    }

    // ── Formato guardado ─────────────────────────────────────────────────────

    @Test
    fun un_intento_se_guarda_y_se_vuelve_a_leer_igual() {
        val row = attempt(title = "Título con ; punto y coma y \"comillas\"", completed = false, errors = 4, hints = 2)
        assertEquals(row, AttemptRow.parse(row.toCsvLine().trimEnd('\r', '\n')))
    }

    @Test
    fun una_linea_danada_se_ignora() {
        assertNull(AttemptRow.parse("P01;solo;tres"))
        val line = attempt().toCsvLine().trimEnd('\r', '\n')
        assertEquals(attempt(), AttemptRow.parse(line))
        assertNull(AttemptRow.parse(line.replace(";10000;", ";no-es-numero;")))
    }

    // ── Informe ──────────────────────────────────────────────────────────────

    @Test
    fun cuenta_los_intentos_hasta_el_primero_que_completo() {
        val rows = dataRows(
            listOf(
                attempt(seconds = 40, completed = false, hits = 1, errors = 3, stars = 0, xp = 0),
                attempt(seconds = 60, completed = true, hits = 3, errors = 1, stars = 2, xp = 100),
                // Repaso posterior: no debe sumarse
                attempt(seconds = 500, completed = true, hits = 3, errors = 0, stars = 3, xp = 0)
            )
        )
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("Completado", row.col("Resultado"))
        assertEquals("2", row.col("Intentos"))
        assertEquals("100", row.col("Tiempo (segundos)"))
        assertEquals("1 min 40 s", row.col("Tiempo"))
        assertEquals("4", row.col("Aciertos"))
        assertEquals("4", row.col("Errores"))
        assertEquals("2", row.col("Estrellas"))
        assertEquals("100", row.col("XP ganada"))
    }

    @Test
    fun un_modulo_abandonado_queda_sin_terminar_y_sin_estrellas() {
        val row = dataRows(listOf(attempt(completed = false, stars = 0, xp = 0, errors = 2))).single()
        assertEquals("Sin terminar", row.col("Resultado"))
        assertEquals("1", row.col("Intentos"))
        assertEquals("", row.col("Estrellas"))
    }

    @Test
    fun cada_participante_y_modulo_tiene_su_fila() {
        val other = NpcRegistry.questNpcs[1].moduleId!!
        val rows = dataRows(
            listOf(
                attempt(session = "P01-20261113-0900"),
                attempt(session = "P01-20261113-0900", module = other),
                attempt(session = "P02-20261113-1000")
            )
        )
        assertEquals(listOf("P01", "P01", "P02"), rows.map { it.col("Participante") })
    }

    @Test
    fun el_minijuego_y_la_pregunta_de_un_modulo_van_en_filas_separadas() {
        val rows = dataRows(
            listOf(
                attempt(challenge = "minijuego", type = Challenge.TYPE_CHAT_STORY, stars = 2),
                attempt(challenge = "pregunta", type = Challenge.TYPE_MULTIPLE_CHOICE, errors = 1, stars = 0, xp = 50)
            )
        )
        assertEquals(2, rows.size)
        assertEquals(listOf("Chat de grupo", "Situación real: pregunta"), rows.map { it.col("Reto") })
        // La pregunta mide si acertó a la primera; no da estrellas
        assertEquals("1", rows[1].col("Errores"))
        assertEquals("no aplica", rows[1].col("Estrellas"))
        assertEquals("2", rows[0].col("Estrellas"))
    }

    @Test
    fun lee_intentos_guardados_antes_de_que_hubiera_dos_retos() {
        val old = attempt(challenge = "").toCsvLine().trimEnd('\r', '\n').removeSuffix(";")
        assertEquals(13, ResearchCsv.parseLine(old).size)
        assertEquals(attempt(challenge = ""), AttemptRow.parse(old))
    }

    @Test
    fun identifica_el_modulo_por_su_maestro_y_orden() {
        val row = dataRows(listOf(attempt())).single()
        assertEquals(firstMaster.questOrder.toString(), row.col("Módulo"))
        assertEquals(firstMaster.name, row.col("Jefe"))
    }

    @Test
    fun las_ayudas_solo_aplican_al_detective() {
        val chat = dataRows(listOf(attempt(type = Challenge.TYPE_CHAT_STORY, hints = 0))).single()
        assertEquals("no aplica", chat.col("Ayudas"))
        val spot = dataRows(listOf(attempt(type = Challenge.TYPE_SPOT_THE_CLUES, hints = 2))).single()
        assertEquals("2", spot.col("Ayudas"))
    }

    @Test
    fun el_archivo_abre_en_excel_y_trae_la_guia() {
        val report = ResearchReport.build(listOf(attempt()))
        assertTrue("Debe empezar con BOM para que Excel lea las tildes", report.startsWith("﻿"))
        assertTrue(report.contains("CÓMO LEER ESTE ARCHIVO"))
        assertEquals(ResearchReport.HEADER, ResearchCsv.parseLine(report.removePrefix("﻿").substringBefore("\r\n")))
    }

    @Test
    fun sin_datos_el_informe_solo_trae_encabezado_y_guia() {
        assertTrue(dataRows(emptyList()).isEmpty())
    }
}
