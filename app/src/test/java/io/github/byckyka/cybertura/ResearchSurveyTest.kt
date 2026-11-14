package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.research.ResearchReport
import io.github.byckyka.cybertura.research.ResearchSurvey
import io.github.byckyka.cybertura.research.SurveyRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La encuesta de percepción: nada viene marcado y lo no respondido no cuenta como respuesta. */
class ResearchSurveyTest {

    private val questions = ResearchSurvey.QUESTIONS.size

    private fun survey(session: String, vararg answers: Int) =
        SurveyRow(session, session.substringBefore('-'), 1_800_000_000_000L, answers.toList())

    private fun surveySheet(surveys: List<SurveyRow>) =
        ResearchReport.sheets(emptyList(), surveys).first { it.name == ResearchReport.SURVEY }

    /** Respuestas de la pestaña de encuesta: participante, fecha y una columna por frase. */
    private fun surveyRows(surveys: List<SurveyRow>): List<List<String>> =
        surveySheet(surveys).rows.map { it.take(2 + questions) }

    @Test
    fun una_encuesta_sin_tocar_queda_toda_sin_respuesta() {
        val answers = ResearchSurvey.normalize(emptyList())
        assertEquals(questions, answers.size)
        assertTrue(answers.all { it == ResearchSurvey.NO_ANSWER })
    }

    @Test
    fun solo_se_aceptan_valores_de_la_escala() {
        val answers = ResearchSurvey.normalize(listOf(5, 0, 9, -1, 3, 1, 4, 4))
        assertEquals(listOf(5, 0, 0, 0, 3, 1, 4), answers)
    }

    @Test
    fun las_respuestas_se_guardan_y_se_vuelven_a_leer() {
        val row = survey("P01-20261113-0900", 5, 4, 0, 3, 5, 2, 1)
        assertEquals(row, SurveyRow.parse(row.toCsvLine().trimEnd('\r', '\n')))
        assertNull(SurveyRow.parse("P01;solo"))
    }

    @Test
    fun lo_no_respondido_sale_vacio_en_el_informe_no_como_un_valor() {
        val row = surveyRows(listOf(survey("P01-20261113-0900", 5, 0, 3, 0, 4, 1, 2))).single()
        assertEquals("P01", row[0])
        assertEquals(listOf("5", "", "3", "", "4", "1", "2"), row.drop(2))
    }

    @Test
    fun si_un_participante_responde_dos_veces_cuenta_la_ultima() {
        val rows = surveyRows(
            listOf(
                survey("P01-20261113-0900", 1, 1, 1, 1, 1, 1, 1),
                survey("P02-20261113-1000", 3, 3, 3, 3, 3, 3, 3),
                survey("P01-20261113-0900", 5, 5, 5, 5, 5, 5, 5)
            )
        )
        assertEquals(2, rows.size)
        assertEquals(List(7) { "5" }, rows.first { it[0] == "P01" }.drop(2))
    }

    @Test
    fun cada_frase_va_completa_en_el_encabezado_con_su_promedio() {
        val sheet = surveySheet(listOf(survey("P01-20261113-0900", 5, 4, 0, 3, 5, 4, 3)))
        ResearchSurvey.QUESTIONS.forEach { q -> assertTrue(sheet.header.contains(q)) }
        // Promedio de las 6 respondidas: 24 / 6
        assertEquals(listOf("4.0"), sheet.column("Promedio"))
        assertTrue(surveyRows(emptyList()).isEmpty())
    }

    @Test
    fun hay_una_carita_por_cada_valor_de_la_escala() {
        assertEquals(ResearchSurvey.MAX_ANSWER, ResearchSurvey.FACES.size)
    }
}
