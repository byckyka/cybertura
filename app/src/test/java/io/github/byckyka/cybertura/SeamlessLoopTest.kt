package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.audio.SeamlessLoop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** El bucle del ambiente: recorta el silencio y une el final con el inicio sin saltos. */
class SeamlessLoopTest {

    private val rate = 1000

    /** Rampa suave (sin silencios) de [frames] frames, con [lead] y [trail] frames de silencio. */
    private fun track(frames: Int, lead: Int = 0, trail: Int = 0, channels: Int = 1): ShortArray {
        val pcm = ShortArray((lead + frames + trail) * channels)
        for (f in 0 until frames) {
            for (c in 0 until channels) pcm[(lead + f) * channels + c] = (1000 + f * 2 + c).toShort()
        }
        return pcm
    }

    @Test
    fun recorta_el_silencio_de_los_extremos() {
        val pcm = track(frames = 400, lead = 30, trail = 50)
        val loop = SeamlessLoop.build(pcm, pcm.size, channels = 1, sampleRate = rate)
        val fade = 100 // un cuarto de la pista útil (menor que 1.5 s)
        assertEquals(30 + fade, loop.offset)
        assertEquals(400 - fade, loop.length)
        assertSame("No debe copiar el audio", pcm, loop.samples)
    }

    @Test
    fun el_final_del_bucle_continua_con_su_inicio() {
        val pcm = track(frames = 400, lead = 30, trail = 50)
        val original = pcm.copyOf()
        val loop = SeamlessLoop.build(pcm, pcm.size, channels = 1, sampleRate = rate)

        val last = loop.samples[loop.offset + loop.length - 1]
        val first = loop.samples[loop.offset]
        // Sin fundido, al reiniciar la onda saltaría del final de la rampa a su inicio (~600).
        // Con el fundido la última muestra queda pegada a la primera.
        val rawJump = abs(original[loop.offset + loop.length - 1] - first)
        assertTrue("La pista de prueba debe tener un salto grande sin fundido", rawJump > 500)
        assertTrue("Salto de $last a $first al reiniciar", abs(first - last) <= 40)

        // El cuerpo del bucle (antes del fundido) no se toca
        val fade = 100
        for (i in loop.offset until loop.offset + loop.length - fade) {
            assertEquals(original[i], loop.samples[i])
        }
        // Y el fundido empieza exactamente donde iba la cola original
        val fadeStart = loop.offset + loop.length - fade
        assertEquals(original[fadeStart], loop.samples[fadeStart])
    }

    @Test
    fun el_fundido_nunca_supera_un_segundo_y_medio() {
        val frames = rate * 20
        val pcm = track(frames = frames)
        val loop = SeamlessLoop.build(pcm, pcm.size, channels = 1, sampleRate = rate)
        assertEquals(rate * 3 / 2, loop.offset)
        assertEquals(frames - rate * 3 / 2, loop.length)
    }

    @Test
    fun en_estereo_el_bucle_empieza_y_termina_en_frames_completos() {
        val pcm = track(frames = 400, lead = 10, trail = 10, channels = 2)
        val loop = SeamlessLoop.build(pcm, pcm.size, channels = 2, sampleRate = rate)
        assertEquals((10 + 100) * 2, loop.offset)
        assertEquals(300 * 2, loop.length)
    }

    @Test
    fun solo_usa_las_muestras_validas_del_arreglo() {
        // El decodificador reserva de más: lo que sobra al final no es audio
        val valid = track(frames = 400)
        val pcm = valid.copyOf(valid.size + 500)
        for (i in valid.size until pcm.size) pcm[i] = 9999
        val loop = SeamlessLoop.build(pcm, valid.size, channels = 1, sampleRate = rate)
        assertTrue(loop.offset + loop.length <= valid.size)
    }

    @Test
    fun una_pista_en_silencio_no_falla() {
        val pcm = ShortArray(200)
        val loop = SeamlessLoop.build(pcm, pcm.size, channels = 1, sampleRate = rate)
        assertEquals(0, loop.offset)
        assertEquals(200, loop.length)
    }
}
