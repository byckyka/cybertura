package io.github.byckyka.cybertura.research

/**
 * Encuesta de satisfacción que el niño responde al final de su sesión del piloto. Son los siete
 * ítems de la parte 1 del Anexo F del documento de grado; la parte 2 (preguntas abiertas) se
 * recoge aparte, en papel o entrevista.
 *
 * Cada frase se responde con una de cinco caritas (1 = muy en desacuerdo … 5 = muy de acuerdo). Ninguna viene
 * marcada: una pregunta sin tocar se guarda como "sin respuesta" (0), para que un niño que solo
 * pulsa "Enviar" no cuente como satisfecho. No hay texto libre, así no se recogen datos personales.
 */
object ResearchSurvey {
    const val NO_ANSWER = 0
    const val MAX_ANSWER = 5

    /** Caritas de la escala, de 1 a 5. */
    val FACES = listOf("😞", "🙁", "😐", "🙂", "😄")

    val QUESTIONS = listOf(
        "El videojuego fue fácil de usar.",
        "Las instrucciones fueron claras.",
        "El contenido fue interesante.",
        "Aprendí cosas nuevas.",
        "Me sentí motivado durante la actividad.",
        "El diseño visual fue agradable.",
        "Me gustaría volver a jugarlo."
    )

    const val LOW_LABEL = "Muy en desacuerdo"
    const val HIGH_LABEL = "Muy de acuerdo"

    /** Deja las respuestas con una por pregunta y dentro de la escala; lo demás queda sin respuesta. */
    fun normalize(answers: List<Int>): List<Int> =
        QUESTIONS.indices.map { i -> answers.getOrNull(i)?.takeIf { it in 1..MAX_ANSWER } ?: NO_ANSWER }
}

/** Respuestas de un participante, tal como se guardan en el teléfono. */
data class SurveyRow(
    val sessionId: String,
    val participant: String,
    val sessionStartMs: Long,
    /** Una por pregunta: 1 a 5, o 0 si no la respondió. */
    val answers: List<Int>
) {
    fun toCsvLine(): String =
        ResearchCsv.line(listOf(sessionId, participant, sessionStartMs.toString()) + answers.map { it.toString() })

    companion object {
        fun parse(line: String): SurveyRow? {
            val c = ResearchCsv.parseLine(line)
            if (c.size < 3) return null
            return runCatching {
                SurveyRow(c[0], c[1], c[2].toLong(), ResearchSurvey.normalize(c.drop(3).map { it.toInt() }))
            }.getOrNull()
        }
    }
}
