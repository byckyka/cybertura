package com.example.cybertura.audio

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tramo de un PCM de 16 bits intercalado que se puede repetir sin hueco ni chasquido.
 *
 * El bucle es la zona `[offset, offset + length)` de [samples]. No se copia el audio: el tramo
 * vive dentro del mismo arreglo decodificado, así la pista ocupa memoria una sola vez.
 */
class SeamlessLoop(
    val samples: ShortArray,
    /** Índice de la primera muestra del bucle. */
    val offset: Int,
    /** Cantidad de muestras del bucle (frames × canales). */
    val length: Int,
    val channels: Int,
    val sampleRate: Int
) {
    companion object {
        /** ~ -40 dB: por debajo se considera silencio de relleno del encoder. */
        private const val SILENCE_THRESHOLD = 300

        /**
         * Recorta el silencio de los extremos de las primeras [size] muestras de [pcm] y funde la
         * cola con el inicio, de modo que al volver al principio la onda continúa.
         *
         * Modifica [pcm]: la cola queda mezclada con el inicio (potencia constante). El inicio
         * que se usó para la mezcla queda fuera del bucle, que empieza justo después.
         */
        fun build(pcm: ShortArray, size: Int, channels: Int, sampleRate: Int): SeamlessLoop {
            val frames = size / channels
            fun frameIsSilent(f: Int) =
                (0 until channels).all { abs(pcm[f * channels + it].toInt()) < SILENCE_THRESHOLD }

            var start = 0
            while (start < frames && frameIsSilent(start)) start++
            var end = frames
            while (end > start && frameIsSilent(end - 1)) end--
            val length = end - start
            // Pista entera en silencio: no hay nada que recortar ni fundir
            if (length <= 0) return SeamlessLoop(pcm, 0, frames * channels, channels, sampleRate)

            // Crossfade de hasta 1.5 s (nunca más de un cuarto de la pista)
            val fade = minOf(sampleRate * 3 / 2, length / 4)

            // Últimos `fade` frames: cola que se apaga + inicio que entra
            for (i in 0 until fade) {
                val t = i.toDouble() / fade
                val gainOut = cos(t * Math.PI / 2)
                val gainIn = sin(t * Math.PI / 2)
                val tail = (end - fade + i) * channels
                val head = (start + i) * channels
                for (c in 0 until channels) {
                    val mixed = pcm[tail + c] * gainOut + pcm[head + c] * gainIn
                    pcm[tail + c] = mixed.toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            return SeamlessLoop(
                samples = pcm,
                offset = (start + fade) * channels,
                length = (length - fade) * channels,
                channels = channels,
                sampleRate = sampleRate
            )
        }
    }
}
