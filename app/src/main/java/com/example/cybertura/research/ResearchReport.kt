package com.example.cybertura.research

import com.example.cybertura.data.model.Challenge
import com.example.cybertura.game.engine.NpcRegistry
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

/**
 * Arma el archivo que exporta el investigador: UN solo CSV legible (UTF-8 con BOM), con una fila
 * por participante y módulo y una guía al final que explica cada columna.
 */
object ResearchReport {
    private const val BOM = "﻿"

    val HEADER = listOf(
        "Participante", "Fecha", "Módulo", "Jefe", "Tema", "Reto", "Resultado", "Intentos",
        "Tiempo", "Tiempo (segundos)", "Aciertos", "Errores", "Ayudas", "Estrellas", "XP ganada"
    )

    private val GUIDE = listOf(
        "CÓMO LEER ESTE ARCHIVO",
        "Cada fila es un participante en un reto. Cada módulo tiene dos retos: primero su minijuego y después",
        "  una pregunta de situación real, que mide si aplica lo aprendido. Las filas van en el orden en que jugó.",
        "Participante: código anónimo que puso el investigador (nunca el nombre del niño).",
        "Fecha: día en que se abrió la sesión de ese participante.",
        "Módulo: número del módulo en la aventura (1 a 8). Jefe: personaje que da el reto. Tema: título del módulo.",
        "Reto: el minijuego del módulo o su pregunta de situación real.",
        "Resultado: Completado si terminó el reto; Sin terminar si salió antes de acabarlo.",
        "Intentos: veces que entró al reto hasta completarlo (si lo repitió después de completarlo no se cuenta).",
        "Tiempo: suma de lo que duraron esos intentos, sin contar los ratos con la app en segundo plano o la pantalla apagada.",
        "  Tiempo (segundos) es el mismo dato en número para sacar promedios.",
        "Aciertos: decisiones correctas en todos esos intentos.",
        "Errores: decisiones incorrectas en todos esos intentos.",
        "  Deslizar: carta clasificada al revés. Detective: toque fuera de una pista.",
        "  Chat de grupo: respuesta que no ayuda. Forja de llaves: llave débil o con trampa y respuestas incorrectas.",
        "  Defiende el bosque: escudo perdido (tocar algo seguro o dejar pasar un virus).",
        "  Situación real: opción incorrecta elegida antes de acertar (0 errores = acertó a la primera).",
        "Ayudas: veces que tocó el botón 💡 Ayuda. Solo existe en el minijuego Detective (módulos 4 y 8);",
        "  muestra una pista sobre lo que falta encontrar y cada ayuda resta como 2 errores en las estrellas. En los demás dice no aplica.",
        "Estrellas: de 1 a 3, las del intento en que completó el minijuego. La pregunta no da estrellas (no aplica).",
        "XP ganada: experiencia obtenida en ese reto (solo se gana la primera vez que se completa)."
    )

    /**
     * Una fila por participante y reto. Cuenta los intentos hasta el primero que lo completó
     * (si después lo repitió, esas repeticiones no se suman).
     */
    fun build(
        rows: List<AttemptRow>,
        surveys: List<SurveyRow> = emptyList(),
        tests: List<KnowledgeRow> = emptyList()
    ): String {
        // SimpleDateFormat no es seguro entre hilos: se crea uno por informe
        val day = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        val sb = StringBuilder(BOM)
        sb.append(ResearchCsv.line(HEADER))

        // groupBy respeta el orden de juego
        val groups = rows.groupBy { Triple(it.sessionId, it.moduleId, it.challengeId) }
        for ((_, all) in groups) {
            val firstWin = all.indexOfFirst { it.completed }
            val attempts = if (firstWin >= 0) all.subList(0, firstWin + 1) else all
            val first = attempts.first()
            val npc = NpcRegistry.npcs.firstOrNull { it.moduleId == first.moduleId }
            val seconds = attempts.sumOf { it.durationMs } / 1000
            val hasHelpButton = first.minigameType == Challenge.TYPE_SPOT_THE_CLUES
            val givesStars = first.minigameType != Challenge.TYPE_MULTIPLE_CHOICE
            sb.append(
                ResearchCsv.line(
                    listOf(
                        first.participant,
                        day.format(Date(first.sessionStartMs)),
                        npc?.questOrder?.toString() ?: "",
                        npc?.name ?: "",
                        first.moduleTitle,
                        minigameName(first.minigameType),
                        if (firstWin >= 0) "Completado" else "Sin terminar",
                        attempts.size.toString(),
                        readableTime(seconds),
                        seconds.toString(),
                        attempts.sumOf { it.hits }.toString(),
                        attempts.sumOf { it.errors }.toString(),
                        if (hasHelpButton) attempts.sumOf { it.hints }.toString() else "no aplica",
                        when {
                            !givesStars -> "no aplica"
                            firstWin >= 0 -> attempts.last().stars.toString()
                            else -> ""
                        },
                        attempts.sumOf { it.xp }.toString()
                    )
                )
            )
        }

        appendKnowledgeTests(sb, tests, day)
        appendSurveys(sb, surveys, day)

        sb.append("\r\n")
        GUIDE.forEach { sb.append(ResearchCsv.line(listOf(it))) }
        return sb.toString()
    }

    /**
     * Cuestionario de conocimientos: una fila por participante y momento (antes / después de
     * jugar); si lo respondió más de una vez en el mismo momento, cuenta la última. Cada pregunta
     * sale como 1 (acertó), 0 (falló), NS (eligió "No lo sé") o vacío (no la respondió).
     */
    private fun appendKnowledgeTests(sb: StringBuilder, tests: List<KnowledgeRow>, day: SimpleDateFormat) {
        if (tests.isEmpty()) return
        val questions = KnowledgeTest.QUESTIONS
        sb.append("\r\n")
        sb.append(ResearchCsv.line(listOf("CUESTIONARIO DE CONOCIMIENTOS (1 = acertó; 0 = falló; NS = no lo sé; vacío = no respondió)")))
        sb.append(
            ResearchCsv.line(
                listOf("Participante", "Fecha", "Momento", "Aciertos", "De") + questions.indices.map { "C${it + 1}" }
            )
        )
        val latest = tests.groupBy { it.sessionId to it.moment }.values.map { it.last() }
            .sortedWith(compareBy({ row -> tests.indexOfFirst { it.sessionId == row.sessionId } }, { it.moment.ordinal }))
        for (test in latest) {
            val answers = KnowledgeTest.normalize(test.answers)
            sb.append(
                ResearchCsv.line(
                    listOf(
                        test.participant,
                        day.format(Date(test.sessionStartMs)),
                        test.moment.label,
                        KnowledgeTest.score(answers).toString(),
                        questions.size.toString()
                    ) + answers.mapIndexed { i, answer ->
                        when {
                            answer == KnowledgeTest.NO_ANSWER -> ""
                            answer == KnowledgeTest.dontKnow(i) -> "NS"
                            KnowledgeTest.isCorrect(i, answer) -> "1"
                            else -> "0"
                        }
                    }
                )
            )
        }
        questions.forEachIndexed { i, q ->
            sb.append(ResearchCsv.line(listOf("C${i + 1}: ${q.text} Respuesta correcta: ${q.options[q.correct - 1]}")))
        }
    }

    /**
     * Encuesta de percepción: una fila por participante (si respondió más de una vez, la última).
     * Debajo va el texto de cada pregunta, para leer las columnas P1, P2…
     */
    private fun appendSurveys(sb: StringBuilder, surveys: List<SurveyRow>, day: SimpleDateFormat) {
        if (surveys.isEmpty()) return
        val questions = ResearchSurvey.QUESTIONS
        sb.append("\r\n")
        sb.append(ResearchCsv.line(listOf("ENCUESTA DE PERCEPCIÓN (1 = nada … 5 = muchísimo; vacío = no respondió)")))
        sb.append(ResearchCsv.line(listOf("Participante", "Fecha") + questions.indices.map { "P${it + 1}" }))
        val latest = surveys.groupBy { it.sessionId }.values.map { it.last() }
        for (survey in latest) {
            val answers = ResearchSurvey.normalize(survey.answers)
            sb.append(
                ResearchCsv.line(
                    listOf(survey.participant, day.format(Date(survey.sessionStartMs))) +
                        answers.map { if (it == ResearchSurvey.NO_ANSWER) "" else it.toString() }
                )
            )
        }
        questions.forEachIndexed { i, question -> sb.append(ResearchCsv.line(listOf("P${i + 1}: $question"))) }
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
