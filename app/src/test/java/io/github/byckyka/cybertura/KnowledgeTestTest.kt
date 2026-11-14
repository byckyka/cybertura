package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.research.KnowledgeRow
import io.github.byckyka.cybertura.research.KnowledgeTest
import io.github.byckyka.cybertura.research.ResearchReport
import io.github.byckyka.cybertura.research.TestMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cuestionario de conocimientos del piloto: mismas preguntas antes y después de jugar. */
class KnowledgeTestTest {

    private val questions = KnowledgeTest.QUESTIONS
    private val allCorrect = questions.map { it.correct }
    private val allDontKnow = questions.indices.map { KnowledgeTest.dontKnow(it) }

    private fun row(session: String, moment: TestMoment, answers: List<Int>) =
        KnowledgeRow(session, session.substringBefore('-'), 1_800_000_000_000L, moment, answers)

    private fun sheet(tests: List<KnowledgeRow>, name: String) =
        ResearchReport.sheets(emptyList(), tests = tests).first { it.name == name }

    /** Filas de la pestaña del cuestionario en el informe. */
    private fun testRows(tests: List<KnowledgeRow>): List<List<String>> = sheet(tests, ResearchReport.KNOWLEDGE).rows

    // ── Contenido ────────────────────────────────────────────────────────────

    @Test
    fun hay_una_pregunta_por_modulo_y_todas_estan_bien_formadas() {
        assertEquals(8, questions.size)
        questions.forEachIndexed { i, q ->
            assertTrue("Pregunta ${i + 1} sin texto", q.text.isNotBlank())
            assertEquals("Pregunta ${i + 1}: deben ser 3 opciones", 3, q.options.size)
            assertTrue("Pregunta ${i + 1}: correcta fuera de rango", q.correct in 1..q.options.size)
            assertEquals("Pregunta ${i + 1}: opciones repetidas", q.options.size, q.options.toSet().size)
        }
    }

    @Test
    fun la_respuesta_correcta_no_esta_siempre_en_la_misma_posicion() {
        val positions = questions.groupingBy { it.correct }.eachCount()
        assertEquals("Deben usarse las 3 posiciones", 3, positions.size)
        assertTrue("Ninguna posición debe concentrar más de la mitad: $positions", positions.values.all { it <= 4 })
    }

    // ── Puntaje ──────────────────────────────────────────────────────────────

    @Test
    fun el_puntaje_cuenta_solo_las_respuestas_correctas() {
        assertEquals(8, KnowledgeTest.score(allCorrect))
        assertEquals(0, KnowledgeTest.score(allDontKnow))
        assertEquals(0, KnowledgeTest.score(emptyList()))
        val oneWrong = allCorrect.toMutableList().also { it[0] = if (questions[0].correct == 1) 2 else 1 }
        assertEquals(7, KnowledgeTest.score(oneWrong))
    }

    @Test
    fun no_lo_se_es_una_respuesta_valida_y_los_valores_raros_quedan_sin_respuesta() {
        assertEquals(allDontKnow, KnowledgeTest.normalize(allDontKnow))
        assertEquals(List(8) { KnowledgeTest.NO_ANSWER }, KnowledgeTest.normalize(List(8) { 99 }))
        assertEquals(8, KnowledgeTest.normalize(listOf(1)).size)
    }

    // ── Guardado e informe ───────────────────────────────────────────────────

    @Test
    fun las_respuestas_se_guardan_y_se_vuelven_a_leer() {
        val saved = row("P01-20261113-0900", TestMoment.POST, allCorrect)
        assertEquals(saved, KnowledgeRow.parse(saved.toCsvLine().trimEnd('\r', '\n')))
        assertNull(KnowledgeRow.parse("P01;P01;123;momento-raro;1;2"))
        assertNull(KnowledgeRow.parse("incompleta"))
    }

    @Test
    fun el_informe_trae_una_fila_antes_y_otra_despues_por_participante() {
        val rows = testRows(
            listOf(
                row("P01-20261113-0900", TestMoment.PRE, allDontKnow),
                row("P02-20261113-1000", TestMoment.PRE, allCorrect),
                row("P01-20261113-0900", TestMoment.POST, allCorrect)
            )
        )
        assertEquals(
            listOf("P01" to "Antes de jugar", "P01" to "Después de jugar", "P02" to "Antes de jugar"),
            rows.map { it[0] to it[2] }
        )
        // Aciertos y total
        assertEquals(listOf("0", "8", "8"), rows.map { it[3] })
        assertTrue(rows.all { it[4] == "8" })
    }

    @Test
    fun cada_pregunta_sale_como_acierto_fallo_no_se_o_vacio() {
        val wrongFirst = if (questions[0].correct == 1) 2 else 1
        val answers = listOf(questions[0].correct, wrongFirst.let { if (questions[1].correct == 1) 2 else 1 },
            KnowledgeTest.dontKnow(2), KnowledgeTest.NO_ANSWER) + allCorrect.drop(4)
        val row = testRows(listOf(row("P01-20261113-0900", TestMoment.PRE, answers))).single()
        assertEquals(
            listOf(ResearchReport.CORRECT, ResearchReport.WRONG, ResearchReport.DONT_KNOW, ""),
            row.drop(5).take(4)
        )
    }

    @Test
    fun si_se_repite_un_momento_cuenta_el_ultimo() {
        val rows = testRows(
            listOf(
                row("P01-20261113-0900", TestMoment.POST, allDontKnow),
                row("P01-20261113-0900", TestMoment.POST, allCorrect)
            )
        )
        assertEquals("8", rows.single()[3])
    }

    @Test
    fun cada_pregunta_va_completa_en_el_encabezado_y_la_guia_trae_la_respuesta_correcta() {
        val tests = listOf(row("P01-20261113-0900", TestMoment.PRE, allCorrect))
        val header = sheet(tests, ResearchReport.KNOWLEDGE).header
        val guide = sheet(tests, ResearchReport.GUIDE_SHEET).rows.joinToString("\n") { it.single() }
        questions.forEachIndexed { i, q ->
            assertTrue(header.contains("${i + 1}. ${q.text}"))
            assertTrue(guide.contains("${q.text} → ${q.options[q.correct - 1]}"))
        }
        assertTrue(testRows(emptyList()).isEmpty())
    }

    @Test
    fun el_resumen_muestra_la_mejora_entre_antes_y_despues() {
        val summary = sheet(
            listOf(
                row("P01-20261113-0900", TestMoment.PRE, allDontKnow),
                row("P01-20261113-0900", TestMoment.POST, allCorrect),
                row("P02-20261113-1000", TestMoment.PRE, allCorrect)
            ),
            ResearchReport.SUMMARY
        )
        val mejora = summary.header.indexOfFirst { it.startsWith("Mejora") }
        assertEquals(listOf("8", ""), summary.rows.map { it[mejora] })
    }
}
