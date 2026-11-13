package com.example.cybertura

import com.example.cybertura.research.ResearchCsv
import com.example.cybertura.research.ResearchReport
import com.example.cybertura.research.ResearchSurvey
import com.example.cybertura.research.SurveyRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La encuesta de percepción: nada viene marcado y lo no respondido no cuenta como respuesta. */
class ResearchSurveyTest {

    private val questions = ResearchSurvey.QUESTIONS.size

    private fun survey(session: String, vararg answers: Int) =
        SurveyRow(session, session.substringBefore('-'), 1_800_000_000_000L, answers.toList())

    /** Filas de la sección de encuesta del informe, sin título, encabezado ni leyenda. */
    private fun surveyRows(surveys: List<SurveyRow>): List<List<String>> {
        val lines = ResearchReport.build(emptyList(), surveys).removePrefix("﻿").split("\r\n")
        val start = lines.indexOfFirst { it.startsWith("Participante;Fecha;P1") }
        if (start < 0) return emptyList()
        return lines.drop(start + 1).takeWhile { !it.startsWith("P1:") && it.isNotBlank() }.map { ResearchCsv.parseLine(it) }
    }

    @Test
    fun una_encuesta_sin_tocar_queda_toda_sin_respuesta() {
        val answers = ResearchSurvey.normalize(emptyList())
        assertEquals(questions, answers.size)
        assertTrue(answers.all { it == ResearchSurvey.NO_ANSWER })
    }

    @Test
    fun solo_se_aceptan_valores_de_la_escala() {
        val answers = ResearchSurvey.normalize(listOf(5, 0, 9, -1, 3, 1, 4, 4))
        assertEquals(listOf(5, 0, 0, 0, 3, 1), answers)
    }

    @Test
    fun las_respuestas_se_guardan_y_se_vuelven_a_leer() {
        val row = survey("P01-20261113-0900", 5, 4, 0, 3, 5, 2)
        assertEquals(row, SurveyRow.parse(row.toCsvLine().trimEnd('\r', '\n')))
        assertNull(SurveyRow.parse("P01;solo"))
    }

    @Test
    fun lo_no_respondido_sale_vacio_en_el_informe_no_como_un_valor() {
        val row = surveyRows(listOf(survey("P01-20261113-0900", 5, 0, 3, 0, 4, 1))).single()
        assertEquals("P01", row[0])
        assertEquals(listOf("5", "", "3", "", "4", "1"), row.drop(2))
    }

    @Test
    fun si_un_participante_responde_dos_veces_cuenta_la_ultima() {
        val rows = surveyRows(
            listOf(
                survey("P01-20261113-0900", 1, 1, 1, 1, 1, 1),
                survey("P02-20261113-1000", 3, 3, 3, 3, 3, 3),
                survey("P01-20261113-0900", 5, 5, 5, 5, 5, 5)
            )
        )
        assertEquals(2, rows.size)
        assertEquals(listOf("5", "5", "5", "5", "5", "5"), rows.first { it[0] == "P01" }.drop(2))
    }

    @Test
    fun el_informe_explica_cada_pregunta_y_omite_la_seccion_si_nadie_respondio() {
        val withSurvey = ResearchReport.build(emptyList(), listOf(survey("P01-20261113-0900", 4, 4, 4, 4, 4, 4)))
        ResearchSurvey.QUESTIONS.forEachIndexed { i, q -> assertTrue(withSurvey.contains("P${i + 1}: $q")) }
        assertFalse(ResearchReport.build(emptyList()).contains("ENCUESTA DE PERCEPCIÓN"))
    }

    @Test
    fun hay_una_carita_por_cada_valor_de_la_escala() {
        assertEquals(ResearchSurvey.MAX_ANSWER, ResearchSurvey.FACES.size)
    }
}
