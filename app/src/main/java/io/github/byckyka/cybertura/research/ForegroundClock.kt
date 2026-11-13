package io.github.byckyka.cybertura.research

/**
 * Reloj que solo avanza mientras la app está en pantalla.
 *
 * Sirve para medir cuánto tarda un niño en un minijuego sin sumar el rato en que la app quedó
 * en segundo plano (pantalla apagada, otra app, una llamada).
 */
class ForegroundClock(private val now: () -> Long = System::currentTimeMillis) {

    private var hiddenTotalMs = 0L
    private var hiddenSinceMs: Long? = null

    /** La app dejó de verse: el reloj se detiene. */
    @Synchronized
    fun onBackground() {
        if (hiddenSinceMs == null) hiddenSinceMs = now()
    }

    /** La app volvió a verse: el reloj sigue donde iba. */
    @Synchronized
    fun onForeground() {
        hiddenSinceMs?.let { hiddenTotalMs += now() - it }
        hiddenSinceMs = null
    }

    /**
     * Marca de tiempo en milisegundos que no cuenta los ratos en segundo plano. La duración de
     * algo es la resta de dos marcas.
     */
    @Synchronized
    fun activeMs(): Long {
        val current = now()
        val hiddenNow = hiddenSinceMs?.let { current - it } ?: 0L
        return current - hiddenTotalMs - hiddenNow
    }
}
