package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.audio.Narrator
import io.github.byckyka.cybertura.game.engine.PlayerCharacters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El alias del jugador y el texto que se le pasa a la voz. */
class ProfileAndNarrationTest {

    // ── Alias ────────────────────────────────────────────────────────────────

    @Test
    fun el_alias_es_una_sola_palabra_corta() {
        assertEquals("Luna", PlayerCharacters.sanitizeName("  Luna  "))
        assertEquals("Rayo7", PlayerCharacters.sanitizeName("Rayo7"))
        assertEquals(PlayerCharacters.MAX_NAME_LENGTH, PlayerCharacters.sanitizeName("a".repeat(40)).length)
    }

    @Test
    fun el_alias_no_deja_escribir_un_nombre_completo_ni_un_contacto() {
        // Sin espacios no cabe "nombre apellido"; sin símbolos no cabe un correo ni un teléfono con formato
        assertEquals("JuanPerezGom", PlayerCharacters.sanitizeName("Juan Perez Gomez"))
        assertFalse(PlayerCharacters.sanitizeName("ana@correo.com").contains('@'))
        assertFalse(PlayerCharacters.sanitizeName("ana@correo.com").contains('.'))
        assertEquals("3001234567", PlayerCharacters.sanitizeName("+57 300-123-4567").takeLast(10))
    }

    @Test
    fun el_alias_acepta_tildes_y_enie() {
        assertEquals("Ñandú", PlayerCharacters.sanitizeName("Ñandú"))
    }

    @Test
    fun sin_alias_se_usa_el_de_siempre() {
        assertEquals(PlayerCharacters.DEFAULT_NAME, PlayerCharacters.finalName(""))
        assertEquals(PlayerCharacters.DEFAULT_NAME, PlayerCharacters.finalName("  !!  "))
        assertEquals("Luna", PlayerCharacters.finalName("Luna"))
    }

    @Test
    fun un_personaje_desconocido_cae_en_el_de_siempre() {
        assertEquals(PlayerCharacters.default, PlayerCharacters.byId(""))
        assertEquals(PlayerCharacters.default, PlayerCharacters.byId(null))
        assertEquals(PlayerCharacters.default, PlayerCharacters.byId("no-existe"))
        PlayerCharacters.all.forEach { assertEquals(it, PlayerCharacters.byId(it.id)) }
        assertEquals(PlayerCharacters.all.size, PlayerCharacters.all.map { it.id }.toSet().size)
    }

    // ── Narración ────────────────────────────────────────────────────────────

    @Test
    fun la_voz_no_lee_emojis_ni_simbolos_decorativos() {
        assertEquals("Tu contraseña es una llave", Narrator.speakable("🔑 Tu contraseña es una llave"))
        assertEquals("¡Final perfecto!", Narrator.speakable("¡Final perfecto! ★★★"))
        assertEquals("Toca los virus", Narrator.speakable("👆 Toca los virus ⚔️"))
        assertEquals("Elegir otra vez", Narrator.speakable("⏪ Elegir otra vez"))
    }

    @Test
    fun la_voz_conserva_tildes_signos_y_numeros() {
        val text = "¿Qué haces? Usa GatoAzulSalta2026! y no la compartas."
        assertEquals(text, Narrator.speakable(text))
    }

    @Test
    fun la_voz_lee_el_texto_de_las_pistas_sin_sus_marcas() {
        assertEquals(
            "Hola, vivo en Calle 10 y estudio aquí",
            Narrator.speakable("Hola, vivo en [[addr|Calle 10]] y estudio aquí")
        )
    }

    @Test
    fun un_texto_de_solo_emojis_queda_vacio() {
        assertTrue(Narrator.speakable("🎁🎮✨").isEmpty())
    }
}
