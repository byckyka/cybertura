package com.example.cybertura

import com.example.cybertura.data.model.ArcadeItem
import com.example.cybertura.data.model.ArcadeWave
import com.example.cybertura.game.engine.ArcadeOutcome
import com.example.cybertura.game.engine.ArcadeWaveSim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Reglas del arcade "Defiende el bosque": qué cuenta como acierto, cuándo se pierde un escudo. */
class ArcadeWaveSimTest {

    private fun item(id: String, threat: Boolean) =
        ArcadeItem(id = id, emoji = "🔹", text = id, isThreat = threat, feedback = "porque sí")

    private val threats = listOf(item("virus", true), item("troyano", true))
    private val safes = listOf(item("tarea", false), item("foto", false))
    private val pool = threats + safes

    private fun sim(
        count: Int = 6,
        pool: List<ArcadeItem> = this.pool,
        shields: Int = 3,
        fallMs: Long = 1000,
        spawnMs: Long = 500,
        seed: Int = 7
    ) = ArcadeWaveSim(ArcadeWave(count = count, fallMs = fallMs, spawnMs = spawnMs), pool, shields, Random(seed))

    /** Avanza en pasos de un cuadro (16 ms) hasta que la oleada termina, aplicando [onStep]. */
    private fun ArcadeWaveSim.play(onStep: (ArcadeWaveSim) -> Unit = {}) {
        var guard = 0
        while (!isOver) {
            advance(16)
            onStep(this)
            check(++guard < 100_000) { "La oleada no termina" }
        }
    }

    // ── Armado de la oleada ──────────────────────────────────────────────────

    @Test
    fun la_oleada_trae_la_cantidad_pedida_y_no_repite_carril_seguido() {
        val spawns = sim(count = 40).spawns
        assertEquals(40, spawns.size)
        assertTrue(spawns.all { it.lane in 0 until ArcadeWaveSim.LANES })
        spawns.zipWithNext().forEach { (a, b) -> assertNotEquals("Carril repetido en ${b.uid}", a.lane, b.lane) }
        // Aparecen en orden y separados por el intervalo de la oleada
        spawns.zipWithNext().forEach { (a, b) -> assertEquals(500L, b.spawnAt - a.spawnAt) }
    }

    @Test
    fun la_oleada_mezcla_amenazas_y_objetos_seguros() {
        val spawns = sim(count = 60).spawns
        val threatShare = spawns.count { it.item.isThreat } / 60f
        assertTrue("Proporción de amenazas: $threatShare", threatShare in 0.4f..0.8f)
    }

    @Test
    fun sin_objetos_seguros_todo_son_amenazas_y_viceversa() {
        assertTrue(sim(pool = threats).spawns.all { it.item.isThreat })
        assertTrue(sim(pool = safes).spawns.none { it.item.isThreat })
    }

    // ── Reglas ───────────────────────────────────────────────────────────────

    @Test
    fun jugar_perfecto_no_pierde_escudos() {
        val sim = sim()
        val outcomes = mutableListOf<ArcadeOutcome>()
        while (!sim.isOver) {
            outcomes += sim.advance(16).map { it.outcome }
            // Toca cada amenaza apenas aparece y deja pasar lo seguro
            sim.spawns.filter { it.item.isThreat && sim.isFalling(it) }
                .forEach { outcomes += sim.tap(it.uid)!!.outcome }
        }
        assertFalse(sim.failed)
        assertEquals(0, sim.shieldsLost)
        assertEquals(3, sim.shields)
        assertEquals(sim.spawns.size, sim.resolvedCount)
        assertTrue(outcomes.all { it == ArcadeOutcome.DESTROYED || it == ArcadeOutcome.PASSED })
        assertEquals(sim.spawns.count { it.item.isThreat }, outcomes.count { it == ArcadeOutcome.DESTROYED })
    }

    @Test
    fun dejar_pasar_las_amenazas_acaba_los_escudos_y_pierde_la_oleada() {
        val sim = sim(count = 10, pool = threats, shields = 3)
        sim.play()
        assertTrue(sim.failed)
        assertEquals(0, sim.shields)
        // Una oleada perdida cuenta todos sus escudos
        assertEquals(3, sim.shieldsLost)
        // Se detiene al perder: no sigue resolviendo objetos
        assertEquals(3, sim.resolvedCount)
        assertTrue(sim.advance(10_000).isEmpty())
    }

    @Test
    fun tocar_algo_seguro_cuesta_un_escudo() {
        val sim = sim(pool = safes)
        sim.advance(ArcadeWaveSim.FIRST_SPAWN_MS)
        val event = sim.tap(sim.spawns.first().uid)!!
        assertEquals(ArcadeOutcome.WRONG_TAP, event.outcome)
        assertTrue(event.outcome.tapped)
        assertEquals(2, sim.shields)
        assertEquals(1, sim.shieldsLost)
    }

    @Test
    fun dejar_pasar_algo_seguro_esta_bien() {
        val sim = sim(count = 4, pool = safes)
        sim.play()
        assertFalse(sim.failed)
        assertEquals(0, sim.shieldsLost)
    }

    @Test
    fun un_objeto_llega_al_bosque_justo_al_terminar_su_caida() {
        val sim = sim(count = 1, pool = threats, fallMs = 1000)
        val landsAt = ArcadeWaveSim.FIRST_SPAWN_MS + 1000
        assertTrue(sim.advance(landsAt - 1).isEmpty())
        assertEquals(ArcadeOutcome.HIT, sim.advance(1).single().outcome)
    }

    // ── Toques que no cuentan ────────────────────────────────────────────────

    @Test
    fun no_se_puede_tocar_un_objeto_que_aun_no_aparece() {
        val sim = sim()
        assertNull(sim.tap(sim.spawns.first().uid))
        assertNull(sim.tap(sim.spawns.last().uid))
        assertEquals(0, sim.resolvedCount)
    }

    @Test
    fun tocar_dos_veces_el_mismo_objeto_cuenta_una_sola_vez() {
        val sim = sim(pool = safes)
        sim.advance(ArcadeWaveSim.FIRST_SPAWN_MS)
        val uid = sim.spawns.first().uid
        sim.tap(uid)
        assertNull(sim.tap(uid))
        assertEquals(2, sim.shields)
    }

    @Test
    fun un_objeto_desconocido_o_un_toque_tras_terminar_se_ignoran() {
        val sim = sim(count = 2, pool = safes)
        assertNull(sim.tap(999))
        sim.play()
        assertNull(sim.tap(sim.spawns.first().uid))
    }

    // ── Avance de la caída ───────────────────────────────────────────────────

    @Test
    fun el_avance_de_la_caida_va_de_cero_a_uno() {
        val sim = sim(count = 1, pool = safes, fallMs = 1000)
        val spawn = sim.spawns.single()
        assertFalse(sim.isFalling(spawn))
        sim.advance(ArcadeWaveSim.FIRST_SPAWN_MS)
        assertTrue(sim.isFalling(spawn))
        assertEquals(0f, sim.progress(spawn), 0.001f)
        sim.advance(500)
        assertEquals(0.5f, sim.progress(spawn), 0.001f)
    }

    @Test
    fun una_oleada_vacia_termina_de_inmediato() {
        val sim = sim(count = 0)
        assertTrue(sim.isOver)
        assertFalse(sim.failed)
    }
}
