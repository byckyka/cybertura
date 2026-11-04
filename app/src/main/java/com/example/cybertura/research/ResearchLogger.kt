package com.example.cybertura.research

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import com.example.cybertura.data.model.Challenge
import com.example.cybertura.game.engine.NpcRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado del modo investigador que se muestra en el panel. */
data class ResearchStatus(
    /** Código del participante de la sesión activa, o null si no se está registrando. */
    val participant: String? = null,
    val sessionStartMs: Long = 0L,
    val attemptsInSession: Int = 0,
    /** Participantes (sesiones) con datos guardados en el teléfono, contando la activa. */
    val savedSessions: Int = 0
) {
    val isRecording: Boolean get() = participant != null
}

/**
 * Registro anónimo de resultados para el piloto (modo investigador).
 *
 * Solo registra mientras hay una sesión activa, que el investigador abre con un código de
 * participante (ej. "P01"). No guarda nombres, ubicación ni identificadores del teléfono, y nada
 * sale del dispositivo hasta que el investigador lo exporta.
 *
 * Internamente guarda una línea por intento de minijuego. Al exportar se genera UN solo CSV
 * legible (separador ";" y UTF-8 con BOM, para que Excel en español lo abra bien): una fila por
 * participante y módulo, con una guía al final que explica cada columna.
 */
class ResearchLogger(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val dir = File(appContext.filesDir, DIR).apply { mkdirs() }
    private val attemptsFile = File(dir, "intentos_v2.csv")

    /** Las escrituras van en un solo hilo, en orden, sin bloquear la interfaz. */
    private val io = Executors.newSingleThreadExecutor()

    private val _status = MutableStateFlow(ResearchStatus())
    val status: StateFlow<ResearchStatus> = _status.asStateFlow()

    init {
        // Formato anterior (3 archivos con columnas técnicas): ya no se usa
        listOf("sesiones.csv", "intentos.csv", "respuestas.csv").forEach { File(dir, it).delete() }
        publishStatus()
    }

    // ── Sesión ───────────────────────────────────────────────────────────────

    val isRecording: Boolean get() = prefs.getString(K_SESSION, null) != null

    /** Abre la sesión de un participante. Si había otra abierta, se cierra. */
    fun startSession(participantCode: String) {
        val now = System.currentTimeMillis()
        val code = sanitizeCode(participantCode)
        prefs.edit()
            .putString(K_CODE, code)
            .putString(K_SESSION, "$code-${FILE_STAMP.format(Date(now))}")
            .putLong(K_START, now)
            .apply()
        publishStatus()
    }

    /** Cierra la sesión activa: a partir de aquí no se registra nada. */
    fun endSession() {
        prefs.edit().remove(K_CODE).remove(K_SESSION).remove(K_START).apply()
        publishStatus()
    }

    // ── Intentos de minijuego ────────────────────────────────────────────────

    /** Empieza a registrar un intento; devuelve null si no hay sesión activa. */
    fun startAttempt(moduleId: String, moduleTitle: String, minigameType: String): Attempt? {
        val sessionId = prefs.getString(K_SESSION, null) ?: return null
        return Attempt(
            sessionId = sessionId,
            participant = prefs.getString(K_CODE, "") ?: "",
            sessionStartMs = prefs.getLong(K_START, 0L),
            moduleId = moduleId,
            moduleTitle = moduleTitle,
            minigameType = minigameType,
            startMs = System.currentTimeMillis()
        )
    }

    inner class Attempt internal constructor(
        private val sessionId: String,
        private val participant: String,
        private val sessionStartMs: Long,
        private val moduleId: String,
        private val moduleTitle: String,
        private val minigameType: String,
        private val startMs: Long
    ) {
        private var hits = 0
        private var closed = false

        /** Una decisión del niño; solo se cuentan los aciertos (los errores los da el minijuego). */
        fun answer(correct: Boolean) {
            if (!closed && correct) hits += 1
        }

        /** El niño terminó el minijuego. */
        fun finish(errors: Int, hints: Int, stars: Int, xpGained: Int) =
            close(completed = true, errors = errors, hints = hints, stars = stars, xpGained = xpGained)

        /** El niño salió del minijuego sin terminarlo. */
        fun abandon(errors: Int, hints: Int) =
            close(completed = false, errors = errors, hints = hints, stars = 0, xpGained = 0)

        private fun close(completed: Boolean, errors: Int, hints: Int, stars: Int, xpGained: Int) {
            if (closed) return
            closed = true
            val row = listOf(
                sessionId, participant, sessionStartMs.toString(), moduleId, moduleTitle, minigameType,
                (System.currentTimeMillis() - startMs).toString(),
                if (completed) "1" else "0",
                hits.toString(), errors.toString(), hints.toString(), stars.toString(), xpGained.toString()
            )
            val line = csvLine(row)
            io.execute {
                runCatching { attemptsFile.appendText(line, Charsets.UTF_8) }
                    .onFailure { Log.e(TAG, "No se pudo guardar el intento", it) }
            }
            _status.value = _status.value.copy(attemptsInSession = _status.value.attemptsInSession + 1)
        }
    }

    // ── Exportar y borrar ────────────────────────────────────────────────────

    private fun exportName() = "cybertura_resultados_${FILE_STAMP.format(Date())}.csv"

    /** Guarda el CSV en una carpeta elegida por el investigador (ej. Descargas). */
    fun exportToFolder(resolver: ContentResolver, treeUri: Uri): Boolean = runCatching {
        val dirUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )
        val fileUri = DocumentsContract.createDocument(resolver, dirUri, "text/csv", exportName())
            ?: return false
        resolver.openOutputStream(fileUri)?.use { it.write(buildReport()) } ?: return false
        true
    }.onFailure { Log.e(TAG, "No se pudo exportar", it) }.getOrDefault(false)

    /** Intent para enviar el CSV con el menú "Compartir" de Android (Drive, correo…). */
    fun shareIntent(): Intent {
        val outDir = File(appContext.cacheDir, "export").apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(outDir, exportName()).apply { writeBytes(buildReport()) }
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Exportar resultados del piloto")
    }

    /** Borra todos los datos de investigación (también la sesión activa). */
    fun clearAll() {
        flush()
        prefs.edit().clear().apply()
        attemptsFile.delete()
        File(appContext.cacheDir, "export").deleteRecursively()
        _status.value = ResearchStatus()
    }

    // ── El informe ───────────────────────────────────────────────────────────

    private class Row(
        val sessionId: String, val participant: String, val sessionStartMs: Long,
        val moduleId: String, val moduleTitle: String, val minigameType: String,
        val durationMs: Long, val completed: Boolean,
        val hits: Int, val errors: Int, val hints: Int, val stars: Int, val xp: Int
    )

    private fun readRows(): List<Row> {
        flush()
        if (!attemptsFile.exists()) return emptyList()
        return attemptsFile.readLines(Charsets.UTF_8).mapNotNull { line ->
            val c = parseCsvLine(line)
            if (c.size < 13) return@mapNotNull null
            runCatching {
                Row(
                    c[0], c[1], c[2].toLong(), c[3], c[4], c[5], c[6].toLong(), c[7] == "1",
                    c[8].toInt(), c[9].toInt(), c[10].toInt(), c[11].toInt(), c[12].toInt()
                )
            }.getOrNull()
        }
    }

    /**
     * Una fila por participante y módulo. Cuenta los intentos hasta el primero que lo completó
     * (si después lo repitió, esas repeticiones no se suman).
     */
    private fun buildReport(): ByteArray {
        val sb = StringBuilder(BOM)
        sb.append(csvLine(REPORT_HEADER))

        val groups = readRows().groupBy { it.sessionId to it.moduleId } // respeta el orden de juego
        for ((_, all) in groups) {
            val firstWin = all.indexOfFirst { it.completed }
            val attempts = if (firstWin >= 0) all.subList(0, firstWin + 1) else all
            val first = attempts.first()
            val npc = NpcRegistry.npcs.firstOrNull { it.moduleId == first.moduleId }
            val seconds = attempts.sumOf { it.durationMs } / 1000
            val hasHelpButton = first.minigameType == Challenge.TYPE_SPOT_THE_CLUES
            sb.append(
                csvLine(
                    listOf(
                        first.participant,
                        DAY.format(Date(first.sessionStartMs)),
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
        GUIDE.forEach { sb.append(csvLine(listOf(it))) }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private fun publishStatus() {
        val sessionId = prefs.getString(K_SESSION, null)
        val rows = runCatching { readRows() }.getOrDefault(emptyList())
        _status.value = ResearchStatus(
            participant = prefs.getString(K_CODE, null),
            sessionStartMs = prefs.getLong(K_START, 0L),
            attemptsInSession = rows.count { it.sessionId == sessionId },
            savedSessions = (rows.map { it.sessionId }.toSet() + listOfNotNull(sessionId)).size
        )
    }

    /** Espera a que terminen las escrituras pendientes (antes de leer o borrar el archivo). */
    private fun flush() {
        runCatching { io.submit {}.get() }
    }

    companion object {
        private const val TAG = "ResearchLogger"
        private const val PREFS = "research"
        private const val DIR = "research"
        private const val SEP = ";"
        private const val BOM = "﻿"

        /** PIN del modo investigador (5 toques en el título de la Sala de Trofeos). */
        const val RESEARCHER_PIN = "2026"

        private const val K_CODE = "participant"
        private const val K_SESSION = "session_id"
        private const val K_START = "session_start"

        private val REPORT_HEADER = listOf(
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
            "Tiempo: suma de lo que duraron esos intentos. Tiempo (segundos) es el mismo dato en número para sacar promedios.",
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

        private val DAY = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        private val FILE_STAMP = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT)

        /** Solo letras, números y guiones: evita que se escriba un nombre completo con espacios. */
        fun sanitizeCode(raw: String): String =
            raw.trim().uppercase(Locale.ROOT).filter { it.isLetterOrDigit() || it == '-' }.take(12)

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

        private fun csvField(value: String): String {
            val clean = value.replace("\r", " ").replace("\n", " ")
            return if (clean.contains(SEP) || clean.contains('"')) "\"" + clean.replace("\"", "\"\"") + "\"" else clean
        }

        private fun csvLine(fields: List<String>): String = fields.joinToString(SEP) { csvField(it) } + "\r\n"

        /** Lee una línea escrita con [csvLine] (campos entre comillas si llevan ";" o comillas). */
        private fun parseCsvLine(line: String): List<String> {
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
}
