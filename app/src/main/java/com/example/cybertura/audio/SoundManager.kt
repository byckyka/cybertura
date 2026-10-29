package com.example.cybertura.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.cybertura.R

/**
 * Gestor central de música y efectos de sonido.
 *
 * - Ambiente: AmbientLoopPlayer, bucle sin cortes (PCM + crossfade).
 * - Pasos: MediaPlayer en bucle.
 * - Efectos cortos (respuestas, chat, puerta): SoundPool, baja latencia.
 * - Jingles de victoria: MediaPlayer (son más largos que el límite cómodo de SoundPool).
 *
 * Los volúmenes están balanceados para que el ambiente quede de fondo y
 * los efectos de feedback se escuchen claros por encima.
 */
object SoundManager {

    // ── Volúmenes (0f..1f) ───────────────────────────────────────────────────
    /** Ambiente explorando el mundo: presente pero sin tapar los efectos. */
    const val AMBIENT_WORLD = 0.30f
    /** Ambiente en menús / tutorial / medallas. */
    const val AMBIENT_MENU = 0.18f
    /** Ambiente durante un reto: muy bajo para que el jugador se concentre en leer. */
    const val AMBIENT_FOCUS = 0.10f
    /** Silencio total (p. ej. mientras suena el jingle de juego completado). */
    const val AMBIENT_MUTED = 0f

    private const val WALK_VOLUME = 0.35f
    private const val CHAT_VOLUME = 0.55f
    private const val DOOR_VOLUME = 0.70f
    private const val CORRECT_VOLUME = 0.85f
    private const val FAIL_VOLUME = 0.70f
    private const val MODULE_DONE_VOLUME = 0.90f
    private const val GAME_DONE_VOLUME = 1.00f

    private lateinit var appContext: Context
    private lateinit var attributes: AudioAttributes

    private var ambientPlayer: AmbientLoopPlayer? = null
    private var walkPlayer: MediaPlayer? = null
    private var jinglePlayer: MediaPlayer? = null

    private var soundPool: SoundPool? = null
    private var sfxCorrect = 0
    private var sfxFail = 0
    private var sfxChat = 0
    private var sfxDoor = 0

    private var ambientVolume = AMBIENT_MENU
    private var isInForeground = false
    private var wantsWalking = false

    fun init(context: Context) {
        appContext = context.applicationContext
        attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()
            .also { pool ->
                sfxCorrect = pool.load(appContext, R.raw.respuesta_correcta, 1)
                sfxFail = pool.load(appContext, R.raw.fail, 1)
                sfxChat = pool.load(appContext, R.raw.sonido_chat_interaccion, 1)
                sfxDoor = pool.load(appContext, R.raw.sonido_entrar_casa, 1)
            }
    }

    private fun createPlayer(resId: Int, loop: Boolean, volume: Float): MediaPlayer? {
        val sessionId = (appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .generateAudioSessionId()
        return MediaPlayer.create(appContext, resId, attributes, sessionId)?.apply {
            isLooping = loop
            setVolume(volume, volume)
        }
    }

    // ── Ciclo de vida de la app ──────────────────────────────────────────────

    /** Llamar en Activity.onStart: arranca/reanuda el ambiente. */
    fun onAppForeground() {
        isInForeground = true
        val player = ambientPlayer
            ?: AmbientLoopPlayer(appContext, R.raw.musica_ambiente, attributes)
                .also { ambientPlayer = it }
        player.setVolume(ambientVolume)
        player.play()
        if (wantsWalking) walkPlayer?.start()
        jinglePlayer?.takeIf { !it.isPlaying }?.start()
        soundPool?.autoResume()
    }

    /** Llamar en Activity.onStop: pausa todo para no sonar en segundo plano. */
    fun onAppBackground() {
        isInForeground = false
        ambientPlayer?.pause()
        walkPlayer?.takeIf { it.isPlaying }?.pause()
        jinglePlayer?.takeIf { it.isPlaying }?.pause()
        soundPool?.autoPause()
    }

    // ── Ambiente ─────────────────────────────────────────────────────────────

    fun setAmbientVolume(volume: Float) {
        ambientVolume = volume
        ambientPlayer?.setVolume(volume)
    }

    // ── Pasos ────────────────────────────────────────────────────────────────

    fun setWalking(walking: Boolean) {
        wantsWalking = walking
        if (walking) {
            val player = walkPlayer
                ?: createPlayer(R.raw.sonido_caminando, loop = true, volume = WALK_VOLUME)
                    .also { walkPlayer = it }
            if (isInForeground && player != null && !player.isPlaying) player.start()
        } else {
            walkPlayer?.takeIf { it.isPlaying }?.let {
                it.pause()
                it.seekTo(0)
            }
        }
    }

    // ── Efectos cortos ───────────────────────────────────────────────────────

    private fun playSfx(id: Int, volume: Float) {
        if (id != 0) soundPool?.play(id, volume, volume, 1, 0, 1f)
    }

    fun playCorrect() = playSfx(sfxCorrect, CORRECT_VOLUME)
    fun playFail() = playSfx(sfxFail, FAIL_VOLUME)
    fun playChat() = playSfx(sfxChat, CHAT_VOLUME)
    /** Puerta: suena al entrar y al salir de una casa. */
    fun playDoor() = playSfx(sfxDoor, DOOR_VOLUME)

    // ── Jingles de victoria ──────────────────────────────────────────────────

    private fun playJingle(resId: Int, volume: Float) {
        jinglePlayer?.release()
        jinglePlayer = createPlayer(resId, loop = false, volume = volume)?.apply {
            setOnCompletionListener {
                it.release()
                if (jinglePlayer === it) jinglePlayer = null
            }
            start()
        }
    }

    fun playModuleCompleted() = playJingle(R.raw.sonido_modulo_completado, MODULE_DONE_VOLUME)
    fun playGameCompleted() = playJingle(R.raw.sonido_juego_completado, GAME_DONE_VOLUME)
}
