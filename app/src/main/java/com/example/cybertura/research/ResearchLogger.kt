package com.example.cybertura.research

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
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
 * Internamente guarda una línea por intento de minijuego ([AttemptRow]). Al exportar se genera
 * el informe de [ResearchReport].
 */
class ResearchLogger(
    context: Context,
    private val clock: ForegroundClock = ForegroundClock()
) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val dir = File(appContext.filesDir, DIR)
    private val attemptsFile = File(dir, "intentos_v2.csv")

    /**
     * Todo lo que toca el archivo va en un solo hilo, en orden y sin bloquear la interfaz:
     * las escrituras, el recuento que muestra el panel y el borrado.
     */
    private val io = Executors.newSingleThreadExecutor()

    private val _status = MutableStateFlow(ResearchStatus())
    val status: StateFlow<ResearchStatus> = _status.asStateFlow()

    init {
        io.execute {
            dir.mkdirs()
            // Formato anterior (3 archivos con columnas técnicas): ya no se usa
            listOf("sesiones.csv", "intentos.csv", "respuestas.csv").forEach { File(dir, it).delete() }
            publishStatus()
        }
    }

    // ── Ciclo de vida de la app ──────────────────────────────────────────────

    /** Llamar en Activity.onStart: el tiempo de los intentos vuelve a correr. */
    fun onAppForeground() = clock.onForeground()

    /** Llamar en Activity.onStop: el tiempo en segundo plano no se le suma a ningún intento. */
    fun onAppBackground() = clock.onBackground()

    // ── Sesión ───────────────────────────────────────────────────────────────

    val isRecording: Boolean get() = prefs.getString(K_SESSION, null) != null

    /** Abre la sesión de un participante. Si había otra abierta, se cierra. */
    fun startSession(participantCode: String) {
        val now = System.currentTimeMillis()
        val code = sanitizeCode(participantCode)
        prefs.edit()
            .putString(K_CODE, code)
            .putString(K_SESSION, "$code-${fileStamp(now)}")
            .putLong(K_START, now)
            .apply()
        io.execute { publishStatus() }
    }

    /** Cierra la sesión activa: a partir de aquí no se registra nada. */
    fun endSession() {
        prefs.edit().remove(K_CODE).remove(K_SESSION).remove(K_START).apply()
        io.execute { publishStatus() }
    }

    // ── Intentos de minijuego ────────────────────────────────────────────────

    /** Empieza a registrar un intento; devuelve null si no hay sesión activa. */
    fun startAttempt(moduleId: String, moduleTitle: String, challengeId: String, minigameType: String): Attempt? {
        val sessionId = prefs.getString(K_SESSION, null) ?: return null
        return Attempt(
            sessionId = sessionId,
            participant = prefs.getString(K_CODE, "") ?: "",
            sessionStartMs = prefs.getLong(K_START, 0L),
            moduleId = moduleId,
            moduleTitle = moduleTitle,
            challengeId = challengeId,
            minigameType = minigameType,
            startActiveMs = clock.activeMs()
        )
    }

    inner class Attempt internal constructor(
        private val sessionId: String,
        private val participant: String,
        private val sessionStartMs: Long,
        private val moduleId: String,
        private val moduleTitle: String,
        private val challengeId: String,
        private val minigameType: String,
        private val startActiveMs: Long
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
            val line = AttemptRow(
                sessionId = sessionId,
                participant = participant,
                sessionStartMs = sessionStartMs,
                moduleId = moduleId,
                moduleTitle = moduleTitle,
                minigameType = minigameType,
                durationMs = clock.activeMs() - startActiveMs,
                completed = completed,
                hits = hits,
                errors = errors,
                hints = hints,
                stars = stars,
                xp = xpGained,
                challengeId = challengeId
            ).toCsvLine()
            io.execute {
                runCatching { attemptsFile.appendText(line, Charsets.UTF_8) }
                    .onFailure { Log.e(TAG, "No se pudo guardar el intento", it) }
                publishStatus()
            }
        }
    }

    // ── Exportar y borrar ────────────────────────────────────────────────────

    private fun exportName() = "cybertura_resultados_${fileStamp(System.currentTimeMillis())}.csv"

    private fun buildReport(): ByteArray = ResearchReport.build(readRows()).toByteArray(Charsets.UTF_8)

    /** Guarda el CSV en una carpeta elegida por el investigador (ej. Descargas). No llamar en el hilo principal. */
    fun exportToFolder(resolver: ContentResolver, treeUri: Uri): Boolean = runCatching {
        val dirUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )
        val fileUri = DocumentsContract.createDocument(resolver, dirUri, "text/csv", exportName())
            ?: return false
        resolver.openOutputStream(fileUri)?.use { it.write(buildReport()) } ?: return false
        true
    }.onFailure { Log.e(TAG, "No se pudo exportar", it) }.getOrDefault(false)

    /**
     * Intent para enviar el CSV con el menú "Compartir" de Android (Drive, correo…).
     * No llamar en el hilo principal.
     */
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

    /** Borra todos los datos de investigación (también la sesión activa). No llamar en el hilo principal. */
    fun clearAll() {
        prefs.edit().clear().apply()
        io.execute {
            attemptsFile.delete()
            File(appContext.cacheDir, "export").deleteRecursively()
            publishStatus()
        }
        flush()
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    /** Intentos guardados, incluidos los que aún estaban por escribirse. No llamar desde [io]. */
    private fun readRows(): List<AttemptRow> {
        flush()
        return readRowsFromDisk()
    }

    private fun readRowsFromDisk(): List<AttemptRow> {
        if (!attemptsFile.exists()) return emptyList()
        return attemptsFile.readLines(Charsets.UTF_8).mapNotNull { AttemptRow.parse(it) }
    }

    /** Recuenta lo guardado y lo publica para el panel. Se ejecuta siempre dentro de [io]. */
    private fun publishStatus() {
        val sessionId = prefs.getString(K_SESSION, null)
        val rows = runCatching { readRowsFromDisk() }.getOrDefault(emptyList())
        _status.value = ResearchStatus(
            participant = prefs.getString(K_CODE, null),
            sessionStartMs = prefs.getLong(K_START, 0L),
            attemptsInSession = rows.count { it.sessionId == sessionId },
            savedSessions = (rows.map { it.sessionId }.toSet() + listOfNotNull(sessionId)).size
        )
    }

    /** Espera a que termine lo pendiente en [io]. No llamar desde [io]: se bloquearía a sí mismo. */
    private fun flush() {
        runCatching { io.submit {}.get() }
    }

    companion object {
        private const val TAG = "ResearchLogger"
        private const val PREFS = "research"
        private const val DIR = "research"

        /** PIN del modo investigador (5 toques en el título de la Sala de Trofeos). */
        const val RESEARCHER_PIN = "2026"

        private const val K_CODE = "participant"
        private const val K_SESSION = "session_id"
        private const val K_START = "session_start"

        /** SimpleDateFormat no es seguro entre hilos: se crea uno por uso. */
        private fun fileStamp(timeMs: Long): String =
            SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date(timeMs))

        /** Solo letras, números y guiones: evita que se escriba un nombre completo con espacios. */
        fun sanitizeCode(raw: String): String =
            raw.trim().uppercase(Locale.ROOT).filter { it.isLetterOrDigit() || it == '-' }.take(12)
    }
}
