package com.example.cybertura.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reproductor de bucle sin cortes para el ambiente.
 *
 * MediaPlayer deja un hueco audible al reiniciar un MP3 (más el silencio que el
 * encoder agrega al inicio y al final). Aquí el MP3 se decodifica una sola vez a PCM,
 * se recorta el silencio de los extremos, se funde el final con el inicio
 * (crossfade) y se escribe en bucle continuo a un AudioTrack.
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
            // Un fallo de audio nunca debe cerrar la app: en el peor caso no hay ambiente
            try { run() } catch (e: Exception) { android.util.Log.w("AmbientLoop", "Ambiente desactivado", e) }
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
        val decoded = decode() ?: return
        val loop = buildSeamlessLoop(decoded.samples, decoded.channels, decoded.sampleRate)

        val channelMask = if (decoded.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minBuffer = AudioTrack.getMinBufferSize(decoded.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(decoded.sampleRate)
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
            val count = minOf(chunk, loop.size - pos)
            val written = audioTrack.write(loop, pos, count)
            if (written < 0) break
            pos += written
            if (pos >= loop.size) pos = 0
        }
    }

    private class Decoded(val samples: ShortArray, val channels: Int, val sampleRate: Int)

    /** Decodifica el recurso completo a PCM 16-bit intercalado. */
    private fun decode(): Decoded? {
        val extractor = MediaExtractor()
        val afd = context.resources.openRawResourceFd(resId)
        try {
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        } finally {
            afd.close()
        }
        val trackIndex = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)

        val codec = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var out = ShortArray(sampleRate * channels * 10)
        var size = 0
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)!!
                    val read = extractor.readSampleData(buffer, 0)
                    if (read < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, read, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    sampleRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                outIndex >= 0 -> {
                    val shorts = codec.getOutputBuffer(outIndex)!!
                        .apply { position(info.offset); limit(info.offset + info.size) }
                        .order(ByteOrder.nativeOrder())
                        .asShortBuffer()
                    val n = shorts.remaining()
                    if (size + n > out.size) out = out.copyOf(maxOf(out.size * 2, size + n))
                    shorts.get(out, size, n)
                    size += n
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        return if (size > 0) Decoded(out.copyOf(size), channels, sampleRate) else null
    }

    /**
     * Recorta el silencio de los extremos y funde la cola con el inicio, de modo que
     * al volver al principio la onda continúa sin hueco ni chasquido.
     */
    private fun buildSeamlessLoop(pcm: ShortArray, channels: Int, sampleRate: Int): ShortArray {
        val threshold = 300 // ~ -40 dB: por debajo se considera silencio de relleno
        val frames = pcm.size / channels
        fun frameIsSilent(f: Int) = (0 until channels).all { abs(pcm[f * channels + it].toInt()) < threshold }

        var start = 0
        while (start < frames && frameIsSilent(start)) start++
        var end = frames
        while (end > start && frameIsSilent(end - 1)) end--
        val length = end - start
        if (length <= 0) return pcm

        // Crossfade de hasta 1.5 s (nunca más de un cuarto de la pista)
        val fade = minOf(sampleRate * 3 / 2, length / 4)
        val loopFrames = length - fade
        val loop = ShortArray(loopFrames * channels)

        // Cuerpo: desde el final del fade-in hasta el final de la pista
        System.arraycopy(pcm, (start + fade) * channels, loop, 0, loopFrames * channels)

        // Últimos `fade` frames: cola que se apaga + inicio que entra (potencia constante)
        for (i in 0 until fade) {
            val t = i.toDouble() / fade
            val gainOut = cos(t * Math.PI / 2)
            val gainIn = sin(t * Math.PI / 2)
            val dst = (loopFrames - fade + i) * channels
            val head = (start + i) * channels
            for (c in 0 until channels) {
                val mixed = loop[dst + c] * gainOut + pcm[head + c] * gainIn
                loop[dst + c] = mixed.toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return loop
    }
}
