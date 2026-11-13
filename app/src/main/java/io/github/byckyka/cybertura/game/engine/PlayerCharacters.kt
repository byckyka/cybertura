package io.github.byckyka.cybertura.game.engine

import io.github.byckyka.cybertura.data.model.UserProgress

/** Personaje que el jugador puede elegir para recorrer la ciudad. */
data class PlayerCharacter(val id: String, val name: String)

/**
 * Personajes disponibles y reglas del alias del jugador.
 *
 * Los personajes usan sprites que ya están en el juego; ninguno es el de un maestro, para que
 * el jugador no se confunda con quien le da un reto.
 */
object PlayerCharacters {
    val all = listOf(
        PlayerCharacter("thief", "Explorador"),
        PlayerCharacter("townsfolk", "Viajero"),
        PlayerCharacter("butcher", "Fortachón")
    )

    val default: PlayerCharacter = all.first()

    /** El personaje guardado, o el de siempre si no hay ninguno (partidas anteriores). */
    fun byId(id: String?): PlayerCharacter = all.firstOrNull { it.id == id } ?: default

    const val MAX_NAME_LENGTH = 12
    const val DEFAULT_NAME = UserProgress.DEFAULT_AGENT_NAME

    /**
     * Deja el alias en una sola palabra corta: letras y números, sin espacios ni símbolos.
     * Así no cabe un nombre completo con apellido, un teléfono con formato ni un correo, que
     * es justo lo que el módulo de privacidad enseña a no compartir.
     */
    fun sanitizeName(raw: String): String =
        raw.filter { it.isLetterOrDigit() }.take(MAX_NAME_LENGTH)

    /** Alias final: el escrito por el jugador o el de siempre si lo dejó vacío. */
    fun finalName(raw: String): String = sanitizeName(raw).ifEmpty { DEFAULT_NAME }
}
