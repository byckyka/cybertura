package io.github.byckyka.cybertura.research

import io.github.byckyka.cybertura.data.model.Challenge
import io.github.byckyka.cybertura.game.engine.NpcRegistry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Un intento de minijuego tal como se guarda en el teléfono: una línea por intento. */
data class AttemptRow(
    val sessionId: String,
    val participant: String,
    val sessionStartMs: Long,
    val moduleId: String,
    val moduleTitle: String,
    val minigameType: String,
    /** Tiempo con la app en pantalla; no cuenta los ratos en segundo plano. */
    val durationMs: Long,
    val completed: Boolean,
    val hits: Int,
    val errors: Int,
    val hints: Int,
    val stars: Int,
    val xp: Int,
    /** Reto del módulo (minijuego o pregunta). Vacío en datos guardados antes de que hubiera dos retos. */
    val challengeId: String = ""
) {
    fun toCsvLine(): String = ResearchCsv.line(
        listOf(
            sessionId, participant, sessionStartMs.toString(), moduleId, moduleTitle, minigameType,
            durationMs.toString(), if (completed) "1" else "0",
            hits.toString(), errors.toString(), hints.toString(), stars.toString(), xp.toString(),
            challengeId
        )
    )

    companion object {
        /** Lee una línea escrita con [toCsvLine]; null si está incompleta o dañada. */
        fun parse(line: String): AttemptRow? {
            val c = ResearchCsv.parseLine(line)
            if (c.size < 13) return null
            return runCatching {
                AttemptRow(
                    c[0], c[1], c[2].toLong(), c[3], c[4], c[5], c[6].toLong(), c[7] == "1",
                    c[8].toInt(), c[9].toInt(), c[10].toInt(), c[11].toInt(), c[12].toInt(),
                    c.getOrElse(13) { "" }
                )
            }.getOrNull()
        }
    }
}

/** CSV con separador ";" (el que Excel en español abre directo). */
object ResearchCsv {
    private const val SEP = ";"

    private fun field(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(SEP) || clean.contains('"')) "\"" + clean.replace("\"", "\"\"") + "\"" else clean
    }

    fun line(fields: List<String>): String = fields.joinToString(SEP) { field(it) } + "\r\n"

    /** Lee una línea escrita con [line] (campos entre comillas si llevan ";" o comillas). */
    fun parseLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                quoted && ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ';' -> { out += cur.toString(); cur.clear() }
                else -> cur.append(ch)
            }
            i++
        }
        out += cur.toString()
        return out
    }
}

/** Una pestaña del informe: encabezado y filas, tal como se ven en Excel o en Google Sheets. */
data class ReportSheet(
    val name: String,
    val header: List<String>,
    val rows: List<List<String>>
) {
    fun column(name: String): List<String> = header.indexOf(name).let { i -> rows.map { it[i] } }
}

/**
 * Arma el informe que exporta el investigador, pensado para leerlo sin saber de código: varias
 * pestañas (resumen por participante, retos, cuestionario, encuesta y una guía), con las
 * preguntas completas en los encabezados y las respuestas en palabras.
 *
 * Las mismas pestañas se guardan como Excel ([XlsxWriter]) o se envían a Google Sheets. Cada fila
 * de datos lleva al final el [SESSION_COLUMN], que Google Sheets usa para reemplazar (y no
 * duplicar) las filas de una sesión cuando se envía otra vez.
 */
object ResearchReport {

    const val SUMMARY = "Resumen"
    const val CHALLENGES = "Retos"
    const val KNOWLEDGE = "Conocimientos"
    const val SURVEY = "Encuesta"
    const val GUIDE_SHEET = "Guía"

    /** Identifica la sesión de un participante; va al final de cada fila de datos. */
    const val SESSION_COLUMN = "ID de sesión"

    const val NOT_APPLICABLE = "no aplica"

    val HEADER = listOf(
        "Participante", "Fecha", "Módulo", "Jefe", "Tema", "Reto", "Resultado", "Intentos",
        "Tiempo", "Tiempo (segundos)", "Aciertos", "Errores", "Ayudas", "Estrellas", "XP ganada",
        SESSION_COLUMN
    )

    val SUMMARY_HEADER = listOf(
        "Participante", "Fecha", "Retos completados", "Tiempo jugando", "Tiempo (minutos)",
        "Errores en total", "Cuestionario antes (aciertos de ${KnowledgeTest.QUESTIONS.size})",
        "Cuestionario después (aciertos de ${KnowledgeTest.QUESTIONS.size})", "Mejora en el cuestionario",
        "Encuesta: promedio (1 a 5)", SESSION_COLUMN
    )

    const val CORRECT = "Acertó"
    const val WRONG = "Falló"
    const val DONT_KNOW = "No sabe"

    private val GUIDE = listOf(
        "Este archivo tiene los resultados del piloto de CyberTura. Ningún dato identifica al niño: cada participante aparece con el código anónimo que puso el investigador (por ejemplo P01).",
        "",
        "PESTAÑA RESUMEN: una fila por participante, con lo más importante.",
        "• Retos completados: cuántos retos terminó (cada módulo tiene un minijuego y una pregunta).",
        "• Tiempo jugando: suma del tiempo en los retos, sin contar los ratos con la app en segundo plano o la pantalla apagada.",
        "• Cuestionario antes / después: respuestas correctas en el cuestionario de conocimientos al empezar y al terminar.",
        "• Mejora en el cuestionario: aciertos de después menos aciertos de antes (positivo = mejoró). Vacío si falta alguno de los dos.",
        "• Encuesta: promedio de las respuestas de la encuesta de satisfacción (1 = muy en desacuerdo, 5 = muy de acuerdo).",
        "",
        "PESTAÑA RETOS: una fila por participante y reto, en el orden en que jugó.",
        "• Módulo: número del módulo en la aventura (1 a 8). Jefe: personaje que da el reto. Tema: título del módulo.",
        "• Reto: el minijuego del módulo o su pregunta de situación real, que mide si aplica lo aprendido.",
        "• Resultado: Completado si terminó el reto; Sin terminar si salió antes de acabarlo.",
        "• Intentos: veces que entró al reto hasta completarlo (si lo repitió después de completarlo no se cuenta).",
        "• Tiempo: suma de lo que duraron esos intentos. Tiempo (segundos) es el mismo dato en número para sacar promedios.",
        "• Aciertos: decisiones correctas en todos esos intentos.",
        "• Errores: decisiones incorrectas en todos esos intentos.",
        "    Deslizar: carta clasificada al revés. Detective: toque fuera de una pista.",
        "    Chat de grupo: respuesta que no ayuda. Forja de llaves: llave débil o con trampa y respuestas incorrectas.",
        "    Defiende el bosque: escudo perdido (tocar algo seguro o dejar pasar un virus).",
        "    Situación real: opción incorrecta elegida antes de acertar (0 errores = acertó a la primera).",
        "• Ayudas: veces que tocó el botón 💡 Ayuda. Solo existe en el minijuego Detective (módulos 4 y 8); cada ayuda resta como 2 errores en las estrellas. En los demás dice \"$NOT_APPLICABLE\".",
        "• Estrellas: de 1 a 3, las del intento en que completó el minijuego. La pregunta no da estrellas (\"$NOT_APPLICABLE\").",
        "• XP ganada: experiencia obtenida en ese reto (solo se gana la primera vez que se completa).",
        "",
        "PESTAÑA CONOCIMIENTOS: el mismo cuestionario antes y después de jugar; una fila por participante y momento (si lo repitió, cuenta el último).",
        "• Cada pregunta sale como $CORRECT, $WRONG, $DONT_KNOW (eligió \"No lo sé\") o vacío (no la respondió).",
        "",
        "PESTAÑA ENCUESTA: la encuesta de satisfacción del final; una fila por participante (si la repitió, cuenta la última).",
        "• Cada frase se responde de 1 (muy en desacuerdo) a 5 (muy de acuerdo). Vacío = no respondió esa frase.",
        "",
        "La columna \"$SESSION_COLUMN\" es interna: distingue dos sesiones del mismo código y evita filas repetidas en Google Sheets.",
        "",
        "RESPUESTAS CORRECTAS DEL CUESTIONARIO:"
    ) + KnowledgeTest.QUESTIONS.mapIndexed { i, q -> "${i + 1}. ${q.text} → ${q.options[q.correct - 1]}" }

    /** Todas las pestañas del informe, en el orden en que se muestran. */
    fun sheets(
        rows: List<AttemptRow>,
        surveys: List<SurveyRow> = emptyList(),
        tests: List<KnowledgeRow> = emptyList()
    ): List<ReportSheet> {
        // SimpleDateFormat no es seguro entre hilos: se crea uno por informe
        val day = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        val latestTests = latestTests(tests)
        val latestSurveys = surveys.groupBy { it.sessionId }.values.map { it.last() }
        return listOf(
            summarySheet(rows, latestTests, latestSurveys, day),
            challengesSheet(rows, day),
            knowledgeSheet(latestTests, day),
            surveySheet(latestSurveys, day),
            ReportSheet(GUIDE_SHEET, listOf("Cómo leer este archivo"), GUIDE.map { listOf(it) })
        )
    }

    /**
     * Una fila por participante y reto. Cuenta los intentos hasta el primero que lo completó
     * (si después lo repitió, esas repeticiones no se suman).
     */
    private fun challengeGroups(rows: List<AttemptRow>): List<List<AttemptRow>> =
        // groupBy respeta el orden de juego
        rows.groupBy { Triple(it.sessionId, it.moduleId, it.challengeId) }.values.map { all ->
            val firstWin = all.indexOfFirst { it.completed }
            if (firstWin >= 0) all.subList(0, firstWin + 1) else all
        }

    private fun challengesSheet(rows: List<AttemptRow>, day: SimpleDateFormat): ReportSheet {
        val data = challengeGroups(rows).map { attempts ->
            val first = attempts.first()
            val completed = attempts.last().completed
            val npc = NpcRegistry.npcs.firstOrNull { it.moduleId == first.moduleId }
            val seconds = attempts.sumOf { it.durationMs } / 1000
            val hasHelpButton = first.minigameType == Challenge.TYPE_SPOT_THE_CLUES
            val givesStars = first.minigameType != Challenge.TYPE_MULTIPLE_CHOICE
            listOf(
                first.participant,
                day.format(Date(first.sessionStartMs)),
                npc?.questOrder?.toString() ?: "",
                npc?.name ?: "",
                first.moduleTitle,
                minigameName(first.minigameType),
                if (completed) "Completado" else "Sin terminar",
                attempts.size.toString(),
                readableTime(seconds),
                seconds.toString(),
                attempts.sumOf { it.hits }.toString(),
                attempts.sumOf { it.errors }.toString(),
                if (hasHelpButton) attempts.sumOf { it.hints }.toString() else NOT_APPLICABLE,
                when {
                    !givesStars -> NOT_APPLICABLE
                    completed -> attempts.last().stars.toString()
                    else -> ""
                },
                attempts.sumOf { it.xp }.toString(),
                first.sessionId
            )
        }
        return ReportSheet(CHALLENGES, HEADER, data)
    }

    /** Si un participante respondió más de una vez en el mismo momento, cuenta la última. */
    private fun latestTests(tests: List<KnowledgeRow>): List<KnowledgeRow> =
        tests.groupBy { it.sessionId to it.moment }.values.map { it.last() }
            .sortedWith(compareBy({ row -> tests.indexOfFirst { it.sessionId == row.sessionId } }, { it.moment.ordinal }))

    private fun knowledgeSheet(tests: List<KnowledgeRow>, day: SimpleDateFormat): ReportSheet {
        val questions = KnowledgeTest.QUESTIONS
        val header = listOf("Participante", "Fecha", "Momento", "Aciertos", "De") +
            questions.mapIndexed { i, q -> "${i + 1}. ${q.text}" } + SESSION_COLUMN
        val data = tests.map { test ->
            val answers = KnowledgeTest.normalize(test.answers)
            listOf(
                test.participant,
                day.format(Date(test.sessionStartMs)),
                test.moment.label,
                KnowledgeTest.score(answers).toString(),
                questions.size.toString()
            ) + answers.mapIndexed { i, answer ->
                when {
                    answer == KnowledgeTest.NO_ANSWER -> ""
                    answer == KnowledgeTest.dontKnow(i) -> DONT_KNOW
                    KnowledgeTest.isCorrect(i, answer) -> CORRECT
                    else -> WRONG
                }
            } + test.sessionId
        }
        return ReportSheet(KNOWLEDGE, header, data)
    }

    private fun surveySheet(surveys: List<SurveyRow>, day: SimpleDateFormat): ReportSheet {
        val header = listOf("Participante", "Fecha") + ResearchSurvey.QUESTIONS + "Promedio" + SESSION_COLUMN
        val data = surveys.map { survey ->
            val answers = ResearchSurvey.normalize(survey.answers)
            listOf(survey.participant, day.format(Date(survey.sessionStartMs))) +
                answers.map { if (it == ResearchSurvey.NO_ANSWER) "" else it.toString() } +
                surveyAverage(answers) + survey.sessionId
        }
        return ReportSheet(SURVEY, header, data)
    }

    /** Una fila por sesión de participante, en el orden en que aparecieron. */
    private fun summarySheet(
        rows: List<AttemptRow>,
        tests: List<KnowledgeRow>,
        surveys: List<SurveyRow>,
        day: SimpleDateFormat
    ): ReportSheet {
        val groups = challengeGroups(rows)
        val sessions = LinkedHashMap<String, Pair<String, Long>>()
        rows.forEach { sessions.putIfAbsent(it.sessionId, it.participant to it.sessionStartMs) }
        tests.forEach { sessions.putIfAbsent(it.sessionId, it.participant to it.sessionStartMs) }
        surveys.forEach { sessions.putIfAbsent(it.sessionId, it.participant to it.sessionStartMs) }

        val data = sessions.map { (sessionId, info) ->
            val mine = groups.filter { it.first().sessionId == sessionId }
            val seconds = mine.sumOf { g -> g.sumOf { it.durationMs } } / 1000
            val pre = tests.firstOrNull { it.sessionId == sessionId && it.moment == TestMoment.PRE }
                ?.let { KnowledgeTest.score(it.answers) }
            val post = tests.firstOrNull { it.sessionId == sessionId && it.moment == TestMoment.POST }
                ?.let { KnowledgeTest.score(it.answers) }
            val survey = surveys.firstOrNull { it.sessionId == sessionId }
            listOf(
                info.first,
                day.format(Date(info.second)),
                mine.count { it.last().completed }.toString(),
                readableTime(seconds),
                String.format(Locale.ROOT, "%.1f", seconds / 60.0),
                mine.sumOf { g -> g.sumOf { it.errors } }.toString(),
                pre?.toString() ?: "",
                post?.toString() ?: "",
                if (pre != null && post != null) (post - pre).toString() else "",
                survey?.let { surveyAverage(ResearchSurvey.normalize(it.answers)) } ?: "",
                sessionId
            )
        }
        return ReportSheet(SUMMARY, SUMMARY_HEADER, data)
    }

    /** Promedio de las frases respondidas, con un decimal; vacío si no respondió ninguna. */
    private fun surveyAverage(answers: List<Int>): String {
        val answered = answers.filter { it != ResearchSurvey.NO_ANSWER }
        return if (answered.isEmpty()) "" else String.format(Locale.ROOT, "%.1f", answered.average())
    }

    private fun minigameName(type: String): String = when (type) {
        Challenge.TYPE_SWIPE_SORT -> "Deslizar: ¿seguro o trampa?"
        Challenge.TYPE_SPOT_THE_CLUES -> "Detective: busca las pistas"
        Challenge.TYPE_CHAT_STORY -> "Chat de grupo"
        Challenge.TYPE_FORGE_KEY -> "Forja de llaves"
        Challenge.TYPE_TAP_DEFENSE -> "Defiende el bosque"
        else -> "Situación real: pregunta"
    }

    private fun readableTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "$m min $s s" else "$s s"
    }
}
