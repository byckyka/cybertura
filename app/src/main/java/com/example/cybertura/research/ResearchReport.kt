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
    val xp: Int
) {
    fun toCsvLine(): String = ResearchCsv.line(
        listOf(
            sessionId, participant, sessionStartMs.toString(), moduleId, moduleTitle, minigameType,
            durationMs.toString(), if (completed) "1" else "0",
            hits.toString(), errors.toString(), hints.toString(), stars.toString(), xp.toString()
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
                    c[8].toInt(), c[9].toInt(), c[10].toInt(), c[11].toInt(), c[12].toInt()
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
        "Participante", "Fecha", "Módulo", "Jefe", "Tema", "Minijuego", "Resultado", "Intentos",
        "Tiempo", "Tiempo (segundos)", "Aciertos", "Errores", "Ayudas", "Estrellas", "XP ganada"
    )

    private val GUIDE = listOf(
        "CÓMO LEER ESTE ARCHIVO",
        "Cada fila es un participante en un módulo. Los módulos aparecen en el orden en que los jugó.",
        "Participante: código anónimo que puso el investigador (nunca el nombre del niño).",
        "Fecha: día en que se abrió la sesión de ese participante.",
        "Módulo: número del módulo en la aventura (1 a 8). Jefe: personaje que da el reto. Tema: título del módulo.",
        "Resultado: Completado si terminó el minijuego; Sin terminar si salió antes de acabarlo.",
        "Intentos: veces que entró al minijuego hasta completarlo (si lo repitió después de completarlo no se cuenta).",
        "Tiempo: suma de lo que duraron esos intentos, sin contar los ratos con la app en segundo plano o la pantalla apagada.",
        "  Tiempo (segundos) es el mismo dato en número para sacar promedios.",
        "Aciertos: decisiones correctas en todos esos intentos.",
        "Errores: decisiones incorrectas en todos esos intentos.",
        "  Deslizar: carta clasificada al revés. Detective: toque fuera de una pista.",
        "  Chat de grupo: respuesta que no ayuda. Forja de llaves: llave débil o con trampa y respuestas incorrectas.",
        "  Defiende el bosque: escudo perdido (tocar algo seguro o dejar pasar un virus).",
        "Ayudas: veces que tocó el botón 💡 Ayuda. Solo existe en el minijuego Detective (módulos 4 y 8);",
        "  muestra una pista sobre lo que falta encontrar y cada ayuda resta como 2 errores en las estrellas. En los demás dice no aplica.",
        "Estrellas: de 1 a 3, las del intento en que lo completó.",
        "XP ganada: experiencia obtenida en ese módulo (solo se gana la primera vez que se completa)."
    )

    /**
     * Una fila por participante y módulo. Cuenta los intentos hasta el primero que lo completó
     * (si después lo repitió, esas repeticiones no se suman).
     */
    fun build(rows: List<AttemptRow>): String {
        // SimpleDateFormat no es seguro entre hilos: se crea uno por informe
        val day = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        val sb = StringBuilder(BOM)
        sb.append(ResearchCsv.line(HEADER))

        val groups = rows.groupBy { it.sessionId to it.moduleId } // respeta el orden de juego
        for ((_, all) in groups) {
            val firstWin = all.indexOfFirst { it.completed }
            val attempts = if (firstWin >= 0) all.subList(0, firstWin + 1) else all
            val first = attempts.first()
            val npc = NpcRegistry.npcs.firstOrNull { it.moduleId == first.moduleId }
            val seconds = attempts.sumOf { it.durationMs } / 1000
            val hasHelpButton = first.minigameType == Challenge.TYPE_SPOT_THE_CLUES
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
                        if (firstWin >= 0) attempts.last().stars.toString() else "",
                        attempts.sumOf { it.xp }.toString()
                    )
                )
            )
        }

        sb.append("\r\n")
        GUIDE.forEach { sb.append(ResearchCsv.line(listOf(it))) }
        return sb.toString()
    }

    private fun minigameName(type: String): String = when (type) {
        Challenge.TYPE_SWIPE_SORT -> "Deslizar: ¿seguro o trampa?"
        Challenge.TYPE_SPOT_THE_CLUES -> "Detective: busca las pistas"
        Challenge.TYPE_CHAT_STORY -> "Chat de grupo"
        Challenge.TYPE_FORGE_KEY -> "Forja de llaves"
        Challenge.TYPE_TAP_DEFENSE -> "Defiende el bosque"
        else -> "Preguntas"
    }

    private fun readableTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "$m min $s s" else "$s s"
    }
}
