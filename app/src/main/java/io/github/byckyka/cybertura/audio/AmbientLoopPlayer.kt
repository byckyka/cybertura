package io.github.byckyka.cybertura.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteOrder

/**
 * Reproductor de bucle sin cortes para el ambiente.
 *
 * MediaPlayer deja un hueco audible al reiniciar un MP3 (más el silencio que el
 * encoder agrega al inicio y al final). Aquí el MP3 se decodifica una sola vez a PCM,
 * se recorta el silencio de los extremos, se funde el final con el inicio
 * (crossfade, ver [SeamlessLoop]) y se escribe en bucle continuo a un AudioTrack.
 */
class AmbientLoopPlayer(
    private val context: Context,
    private val resId: Int,
    private val attributes: AudioAttributes
) {
    @Volatile private var volume = 1f
    @Volatile private var wantsPlaying = false
    @Volatile private var released = false

    private var track: AudioTrack? = null
    private val lock = Object()

    init {
        Thread({
            // Un fallo de audio nunca debe cerrar la app: en el peor caso no hay ambiente.
            // Se captura Throwable porque quedarse sin memoria al decodificar la pista
            // (teléfonos de gama baja) es un Error, no una Exception.
            try {
                run()
            } catch (t: Throwable) {
                Log.w("AmbientLoop", "Ambiente desactivado", t)
            }
        }, "AmbientLoop").apply { isDaemon = true }.start()
    }

    fun play() = synchronized(lock) {
        wantsPlaying = true
        track?.play()
        lock.notifyAll() // despierta el hilo de escritura
    }

    fun pause() = synchronized(lock) {
        wantsPlaying = false
        track?.pause()
    }

    fun setVolume(value: Float) = synchronized(lock) {
        volume = value
        track?.setVolume(value)
    }

    fun release() = synchronized(lock) {
        released = true
        track?.run { pause(); flush(); release() }
        track = null
        lock.notifyAll()
    }

    private fun run() {
        val loop = loadLoop() ?: return
        if (loop.length <= 0) return

        val channelMask = if (loop.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minBuffer = AudioTrack.getMinBufferSize(loop.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(loop.sampleRate)
                    .setChannelMask(channelMask)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuffer * 4)
            .build()

        synchronized(lock) {
            if (released) { audioTrack.release(); return }
            audioTrack.setVolume(volume)
            if (wantsPlaying) audioTrack.play()
            track = audioTrack
        }

        // Escribe el bucle una y otra vez. En pausa, write() no bloquea: devuelve 0 en cuanto
        // el búfer se llena, así que el hilo duerme hasta que play() o release() lo despierten.
        val chunk = 4096
        var pos = 0
        while (!released) {
            synchronized(lock) {
                while (!wantsPlaying && !released) lock.wait()
            }
            if (released) break
            val count = minOf(chunk, loop.length - pos)
            val written = audioTrack.write(loop.samples, loop.offset + pos, count)
            if (written < 0) break
            pos += written
            if (pos >= loop.length) pos = 0
        }
    }

    /**
     * Decodifica la pista y prepara el bucle sobre el mismo arreglo. Va en su propia función
     * para que no quede ninguna referencia extra al PCM mientras suena.
     */
    private fun loadLoop(): SeamlessLoop? {
        val decoded = decode() ?: return null
        return SeamlessLoop.build(decoded.samples, decoded.size, decoded.channels, decoded.sampleRate)
    }

    /** PCM decodificado: solo las primeras [size] muestras de [samples] son válidas. */
    private class Decoded(val samples: ShortArray, val size: Int, val channels: Int, val sampleRate: Int)

    /** Decodifica el recurso completo a PCM 16-bit intercalado. */
    private fun decode(): Decoded? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            context.resources.openRawResourceFd(resId).use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)

            val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            // Se reserva de una vez el tamaño que dice la duración de la pista (+1 s de margen):
            // crecer duplicando llegaba a ocupar varias veces el tamaño real del audio.
            val durationUs =
                if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) inputFormat.getLong(MediaFormat.KEY_DURATION) else 0L
            val samplesPerSecond = sampleRate * channels
            val estimated = durationUs * samplesPerSecond / 1_000_000L
            var out = ShortArray(
                if (estimated in 1..MAX_PREALLOC_SAMPLES) estimated.toInt() + samplesPerSecond else samplesPerSecond * 10
            )
            var size = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)!!
                        val read = extractor.readSampleData(buffer, 0)
                        if (read < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, read, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        channels = decoder.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        sampleRate = decoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    outIndex >= 0 -> {
                        val shorts = decoder.getOutputBuffer(outIndex)!!
                            .apply { position(info.offset); limit(info.offset + info.size) }
                            .order(ByteOrder.nativeOrder())
                            .asShortBuffer()
                        val n = shorts.remaining()
                        if (size + n > out.size) out = out.copyOf(maxOf(out.size + out.size / 4, size + n))
                        shorts.get(out, size, n)
                        size += n
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            return if (size > 0) Decoded(out, size, channels, sampleRate) else null
        } finally {
            // Se liberan siempre, también si la decodificación falla a mitad de camino
            codec?.runCatching { stop() }
            codec?.release()
            extractor.release()
        }
    }

    private companion object {
        /** Tope de la reserva inicial (~10 min en estéreo a 48 kHz): no se cree una duración absurda. */
        const val MAX_PREALLOC_SAMPLES = 60_000_000L
    }
}
