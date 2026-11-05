package com.example.cybertura

import com.example.cybertura.data.model.Challenge
import com.example.cybertura.data.model.ForgeRound
import com.example.cybertura.data.model.GameData
import com.example.cybertura.game.engine.GameWorld
import com.example.cybertura.game.engine.KeyStrength
import com.example.cybertura.game.engine.MapIds
import com.example.cybertura.game.engine.NpcRegistry
import com.example.cybertura.ui.viewmodel.ForgeGameState
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Valida el contenido real (cyber_content.json) y su relación con el mundo: si alguien edita el
 * JSON y deja un minijuego imposible o mal enlazado, `gradlew test` falla y dice dónde.
 */
class ContentValidationTest {

    private val game: GameData = listOf("src/main/assets", "app/src/main/assets")
        .map { File(it, "cyber_content.json") }
        .first { it.exists() }
        .reader(Charsets.UTF_8)
        .use { Gson().fromJson(it, GameData::class.java) }

    private val challenges: List<Challenge> = game.modules.flatMap { it.challenges }

    private fun ofType(type: String) = challenges.filter { it.type == type }

    private fun assertNoProblems(problems: List<String>) =
        assertTrue(problems.joinToString(prefix = "\n", separator = "\n"), problems.isEmpty())

    private fun duplicates(ids: List<String>) = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

    @Test
    fun catalogo_tiene_ocho_modulos_completos_y_2500_xp() {
        assertEquals(8, game.modules.size)
        assertEquals(2500, challenges.sumOf { it.points })
        val problems = game.modules.flatMap { m ->
            buildList {
                if (m.title.isBlank()) add("${m.id}: sin título")
                if (m.badgeId.isBlank()) add("${m.id}: sin medalla")
                if (m.challenges.isEmpty()) add("${m.id}: sin retos")
                m.challenges.forEach { c ->
                    if (c.points <= 0) add("${c.id}: puntos no válidos")
                    if (c.educationalTip.isBlank()) add("${c.id}: sin consejo educativo")
                }
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun identificadores_no_se_repiten() {
        val repeated = duplicates(game.modules.map { it.id }) +
            duplicates(challenges.map { it.id }) +
            duplicates(game.modules.map { it.badgeId })
        assertTrue("Identificadores repetidos: $repeated", repeated.isEmpty())
    }

    @Test
    fun cada_reto_trae_los_datos_de_su_minijuego() {
        val problems = challenges.filterNot { c ->
            when (c.type) {
                Challenge.TYPE_SWIPE_SORT -> !c.cards.isNullOrEmpty()
                Challenge.TYPE_SPOT_THE_CLUES -> !c.clues.isNullOrEmpty() && c.scene != null
                Challenge.TYPE_CHAT_STORY -> !c.chat?.steps.isNullOrEmpty()
                Challenge.TYPE_FORGE_KEY -> !c.forge?.rounds.isNullOrEmpty()
                Challenge.TYPE_TAP_DEFENSE -> !c.arcade?.waves.isNullOrEmpty() && !c.arcade?.items.isNullOrEmpty()
                else -> false
            }
        }.map { "${it.id}: faltan datos para ${it.type}" }
        assertNoProblems(problems)
    }

    @Test
    fun deslizar_tiene_cartas_seguras_y_trampas_con_explicacion() {
        val problems = ofType(Challenge.TYPE_SWIPE_SORT).flatMap { c ->
            val cards = c.cards.orEmpty()
            buildList {
                if (cards.none { it.isSafe }) add("${c.id}: no hay cartas seguras")
                if (cards.all { it.isSafe }) add("${c.id}: no hay trampas")
                duplicates(cards.map { it.id }).forEach { add("${c.id}: carta repetida $it") }
                cards.filter { it.feedback.isBlank() }.forEach { add("${c.id}/${it.id}: sin explicación") }
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun detective_cada_pista_esta_marcada_en_la_escena() {
        val markup = Regex("""\[\[(\w+)\|(.+?)]]""")
        val problems = ofType(Challenge.TYPE_SPOT_THE_CLUES).flatMap { c ->
            val scene = c.scene
            val text = listOfNotNull(scene?.title, scene?.button) + scene?.lines.orEmpty()
            val marked = text.flatMap { line -> markup.findAll(line).map { it.groupValues[1] } }
            val clues = c.clues.orEmpty()
            val ids = clues.map { it.id }
            buildList {
                (ids - marked.toSet()).forEach { add("${c.id}: la pista $it no está marcada en la escena") }
                (marked.toSet() - ids.toSet()).forEach { add("${c.id}: marca $it sin pista") }
                duplicates(ids).forEach { add("${c.id}: pista repetida $it") }
                clues.filter { it.explanation.isBlank() }.forEach { add("${c.id}/${it.id}: sin explicación") }
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun chat_cada_decision_tiene_una_respuesta_buena_y_explicaciones() {
        val problems = ofType(Challenge.TYPE_CHAT_STORY).flatMap { c ->
            c.chat?.steps.orEmpty().flatMapIndexed { i, step ->
                val choices = step.choices.orEmpty()
                buildList {
                    if (choices.isNotEmpty() && choices.none { it.isGood }) add("${c.id} paso ${i + 1}: sin respuesta buena")
                    duplicates(choices.map { it.id }).forEach { add("${c.id} paso ${i + 1}: opción repetida $it") }
                    choices.filter { it.feedback.isBlank() }.forEach { add("${c.id}/${it.id}: sin explicación") }
                }
            }
        }
        assertNoProblems(problems)
    }

    /** La mejor llave posible sin bloques trampa, respetando el máximo de bloques de la forja. */
    private fun bestSafeLevel(round: ForgeRound): Int {
        val blocks = round.blocks.orEmpty().filterNot { it.isTrap }
        return (1 until (1 shl blocks.size))
            .filter { Integer.bitCount(it) <= ForgeGameState.MAX_BLOCKS }
            .maxOfOrNull { mask ->
                KeyStrength.level(blocks.filterIndexed { i, _ -> mask shr i and 1 == 1 }.joinToString("") { it.text })
            } ?: -1
    }

    @Test
    fun forja_toda_ronda_se_puede_ganar() {
        val problems = ofType(Challenge.TYPE_FORGE_KEY).flatMap { c ->
            c.forge?.rounds.orEmpty().flatMap { round ->
                buildList {
                    if (round.isDefend) {
                        val options = round.options.orEmpty()
                        if (options.none { it.isGood }) add("${c.id}/${round.id}: sin opción buena")
                        options.filter { it.feedback.isBlank() }.forEach { add("${c.id}/${round.id}/${it.id}: sin explicación") }
                    } else {
                        // Igual que el juego: sin minLevel en el JSON se exige Hierro
                        val goal = (round.minLevel ?: 2).coerceIn(0, KeyStrength.MAX_LEVEL)
                        val best = bestSafeLevel(round)
                        if (best < goal) add("${c.id}/${round.id}: meta $goal inalcanzable (máximo $best)")
                        duplicates(round.blocks.orEmpty().map { it.id }).forEach { add("${c.id}/${round.id}: bloque repetido $it") }
                    }
                }
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun arcade_tiene_amenazas_objetos_seguros_y_oleadas_validas() {
        val problems = ofType(Challenge.TYPE_TAP_DEFENSE).flatMap { c ->
            val arcade = c.arcade
            val items = arcade?.items.orEmpty()
            buildList {
                if (items.none { it.isThreat }) add("${c.id}: no hay amenazas")
                if (items.all { it.isThreat }) add("${c.id}: no hay objetos seguros")
                if ((arcade?.shields ?: 3) <= 0) add("${c.id}: sin escudos")
                arcade?.waves.orEmpty().forEachIndexed { i, wave ->
                    if (wave.count <= 0) add("${c.id} oleada ${i + 1}: sin objetos")
                }
                duplicates(items.map { it.id }).forEach { add("${c.id}: objeto repetido $it") }
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun cada_modulo_tiene_su_maestro_en_orden_1_a_8() {
        val masters = NpcRegistry.questNpcs
        assertEquals((1..8).toList(), masters.map { it.questOrder })
        assertEquals(game.modules.map { it.id }.toSet(), masters.mapNotNull { it.moduleId }.toSet())
    }

    @Test
    fun portales_conectan_mapas_en_ambos_sentidos_y_llegan_a_suelo_caminable() {
        val portals = GameWorld.portals
        assertEquals(10, portals.size)
        val problems = portals.flatMap { p ->
            buildList {
                if (!GameWorld.isWalkable(p.toMap, p.toX.toInt(), p.toY.toInt())) {
                    add("${p.fromMap} -> ${p.toMap}: llega a un tile no caminable")
                }
                if (portals.none { it.fromMap == p.toMap && it.toMap == p.fromMap }) {
                    add("${p.fromMap} -> ${p.toMap}: no tiene portal de regreso")
                }
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun npc_estan_en_un_mapa_existente_y_sobre_suelo_caminable() {
        val maps = GameWorld.portals.map { it.toMap }.toSet() + MapIds.EXTERIOR
        assertEquals(15, NpcRegistry.npcs.size)
        val problems = NpcRegistry.npcs.flatMap { npc ->
            buildList {
                if (npc.mapId !in maps) add("${npc.id}: mapa desconocido ${npc.mapId}")
                if (!GameWorld.isWalkable(npc.mapId, npc.tileX.toInt(), npc.tileY.toInt())) {
                    add("${npc.id}: está sobre un tile no caminable")
                }
            }
        }
        assertNoProblems(problems)
    }
}
