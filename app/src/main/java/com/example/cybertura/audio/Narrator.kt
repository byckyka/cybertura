package com.example.cybertura.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lee textos en voz alta con la voz del propio teléfono (TextToSpeech), para los niños a los
 * que leer mucho les cuesta. No usa archivos de audio ni conexión.
 *
 * Si el teléfono no tiene una voz en español instalada, [available] queda en false y los
 * botones de narración no se muestran.
 */
class Narrator(context: Context) {

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    /** Texto que se está leyendo ahora mismo, o null si está en silencio. */
    private val _speaking = MutableStateFlow<String?>(null)
    val speaking: StateFlow<String?> = _speaking.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        // Un fallo de la voz nunca debe cerrar la app: en el peor caso no hay narración
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) configure() else Log.w(TAG, "Sin motor de voz ($status)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Narración desactivada", e)
        }
    }

    private fun configure() {
        val engine = tts ?: return
        try {
            val locale = SPANISH_LOCALES.firstOrNull { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
            if (locale == null) {
                Log.w(TAG, "El teléfono no tiene voz en español")
                return
            }
            engine.language = locale
            // Un poco más despacio que el valor normal: se entiende mejor
            engine.setSpeechRate(0.92f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = clear(utteranceId)
                override fun onStop(utteranceId: String?, interrupted: Boolean) = clear(utteranceId)
                @Deprecated("Requerido por la clase base")
                override fun onError(utteranceId: String?) = clear(utteranceId)
            })
            _available.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Narración desactivada", e)
        }
    }

    /** Solo borra el estado si lo que terminó es lo último que se pidió leer. */
    private fun clear(utteranceId: String?) {
        if (_speaking.value == utteranceId) _speaking.value = null
    }

    /** Lee [text]; si ya había algo sonando, lo reemplaza. */
    fun speak(text: String) {
        val engine = tts ?: return
        if (!_available.value) return
        val clean = speakable(text)
        if (clean.isBlank()) return
        _speaking.value = text
        // El propio texto sirve de id: así se sabe qué botón está sonando
        engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun stop() {
        _speaking.value = null
        tts?.stop()
    }

    companion object {
        private const val TAG = "Narrator"

        /** De la más cercana al público del juego a la más general. */
        private val SPANISH_LOCALES = listOf(
            Locale.forLanguageTag("es-CO"), Locale.forLanguageTag("es-US"), Locale.forLanguageTag("es-MX"),
            Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es")
        )

        /**
         * Quita lo que una voz sintética lee mal: emojis y símbolos decorativos (los nombraría
         * uno por uno) y las marcas `[[id|texto]]` de las pistas.
         */
        fun speakable(text: String): String {
            val withoutMarkup = text.replace(Regex("""\[\[\w+\|(.+?)]]"""), "$1")
            val out = StringBuilder()
            var i = 0
            while (i < withoutMarkup.length) {
                val cp = withoutMarkup.codePointAt(i)
                val type = Character.getType(cp)
                val decorative = type == Character.OTHER_SYMBOL.toInt() ||
                    type == Character.SURROGATE.toInt() ||
                    type == Character.NON_SPACING_MARK.toInt() && cp >= 0xFE00 ||
                    type == Character.FORMAT.toInt() ||
                    type == Character.MODIFIER_SYMBOL.toInt() && cp >= 0x1F3FB ||
                    cp in "←→⏪✦★☆✓✗".map { it.code }
                if (!decorative) out.appendCodePoint(cp)
                i += Character.charCount(cp)
            }
            return out.toString().replace(Regex("\\s+"), " ").trim()
        }
    }
}
