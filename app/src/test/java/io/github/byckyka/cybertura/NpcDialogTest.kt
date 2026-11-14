package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.game.engine.DialogAction
import io.github.byckyka.cybertura.game.engine.NpcRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué ofrece cada NPC según el avance del jugador: aceptar su reto, repasarlo o solo conversar. */
class NpcDialogTest {

    private val masters = NpcRegistry.questNpcs
    private val first = masters[0]
    private val second = masters[1]

    private fun completed(count: Int) = masters.take(count).mapNotNull { it.moduleId }.toSet()

    @Test
    fun el_maestro_actual_ofrece_su_reto() {
        val dialog = NpcRegistry.dialogFor(first, completed(0))
        assertEquals(DialogAction.ACCEPT_QUEST, dialog.action)
        assertEquals(first.greetingDialog, dialog.text)
    }

    @Test
    fun un_maestro_que_aun_no_toca_indica_a_donde_ir() {
        val dialog = NpcRegistry.dialogFor(second, completed(0))
        assertEquals(DialogAction.NONE, dialog.action)
        assertTrue(dialog.text, dialog.text.contains(first.name))
    }

    @Test
    fun un_maestro_superado_deja_repasar_su_reto() {
        val dialog = NpcRegistry.dialogFor(first, completed(1))
        assertEquals(DialogAction.REPLAY_QUEST, dialog.action)
        assertEquals(first.completedDialog, dialog.text)
    }

    @Test
    fun con_la_aventura_terminada_todos_los_maestros_dejan_repasar() {
        val all = completed(masters.size)
        masters.forEach { npc ->
            assertEquals(npc.id, DialogAction.REPLAY_QUEST, NpcRegistry.dialogFor(npc, all).action)
        }
    }

    @Test
    fun cada_maestro_tiene_un_mensaje_para_cuando_ya_fue_superado() {
        val missing = masters.filter { it.completedDialog.isBlank() }.map { it.id }
        assertTrue("Maestros sin completedDialog: $missing", missing.isEmpty())
    }

    @Test
    fun los_aldeanos_solo_conversan() {
        val villagers = NpcRegistry.npcs.filter { it.questOrder == null }
        assertTrue(villagers.isNotEmpty())
        villagers.forEach { npc ->
            assertEquals(npc.id, DialogAction.NONE, NpcRegistry.dialogFor(npc, completed(3)).action)
        }
    }

    /** La partida guardada manda: un maestro completado deja repasar aunque el orden haya cambiado. */
    @Test
    fun un_maestro_completado_fuera_de_orden_tambien_deja_repasar() {
        val last = masters.last()
        val dialog = NpcRegistry.dialogFor(last, setOfNotNull(last.moduleId))
        assertEquals(DialogAction.REPLAY_QUEST, dialog.action)
    }
}
