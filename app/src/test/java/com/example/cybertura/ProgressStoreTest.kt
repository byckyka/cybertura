package com.example.cybertura

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cybertura.data.local.DataStoreManager
import com.example.cybertura.data.model.Challenge
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.UserProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** El progreso guardado: puntos, medalla del módulo, mejor marca de estrellas y perfil. */
class ProgressStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val moduleChallenges = setOf("minijuego", "pregunta")

    /** Ejecuta [block] con un almacén real de DataStore sobre un archivo temporal. */
    private fun withStore(block: suspend (DataStoreManager) -> Unit) = runBlocking {
        val job = Job()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
            folder.newFile("progreso.preferences_pb").also { it.delete() }
        }
        try {
            block(DataStoreManager(dataStore))
        } finally {
            job.cancelAndJoin()
        }
    }

    private suspend fun DataStoreManager.complete(challengeId: String, points: Int = 100, stars: Int? = null) =
        recordChallengeSuccess(challengeId, points, "modulo", moduleChallenges, "medalla", stars)

    private suspend fun DataStoreManager.progress(): UserProgress = userProgressFlow.first()

    @Test
    fun una_partida_nueva_empieza_vacia() = withStore { store ->
        val progress = store.progress()
        assertEquals(0, progress.totalScore)
        assertTrue(progress.completedChallengeIds.isEmpty())
        assertTrue(progress.bestStars.isEmpty())
        assertEquals(UserProgress.DEFAULT_AGENT_NAME, progress.agentName)
    }

    @Test
    fun los_puntos_de_un_reto_se_suman_una_sola_vez() = withStore { store ->
        store.complete("minijuego", points = 300, stars = 2)
        store.complete("minijuego", points = 300, stars = 2)
        assertEquals(300, store.progress().totalScore)
    }

    @Test
    fun la_medalla_llega_al_completar_todos_los_retos_del_modulo() = withStore { store ->
        store.complete("minijuego", stars = 3)
        assertFalse(store.progress().isModuleCompleted("modulo"))
        assertTrue(store.progress().unlockedBadgeIds.isEmpty())

        store.complete("pregunta", points = 50)
        val progress = store.progress()
        assertTrue(progress.isModuleCompleted("modulo"))
        assertEquals(setOf("medalla"), progress.unlockedBadgeIds)
        assertEquals(150, progress.totalScore)
    }

    @Test
    fun de_las_estrellas_se_guarda_la_mejor_marca() = withStore { store ->
        store.complete("minijuego", stars = 1)
        assertEquals(1, store.progress().starsFor("minijuego"))

        store.complete("minijuego", stars = 3)
        assertEquals(3, store.progress().starsFor("minijuego"))

        // Un repaso peor no baja la marca
        store.complete("minijuego", stars = 2)
        assertEquals(3, store.progress().starsFor("minijuego"))
    }

    @Test
    fun un_reto_sin_estrellas_no_guarda_marca() = withStore { store ->
        store.complete("pregunta", stars = null)
        assertEquals(0, store.progress().starsFor("pregunta"))
        assertTrue(store.progress().bestStars.isEmpty())
    }

    @Test
    fun las_estrellas_del_modulo_son_las_de_su_minijuego() = withStore { store ->
        val module = CyberModule(
            id = "modulo",
            title = "Módulo",
            challenges = listOf(
                Challenge(id = "minijuego", title = "Juego", type = Challenge.TYPE_CHAT_STORY, question = "?"),
                Challenge(id = "pregunta", title = "Pregunta", question = "?")
            )
        )
        assertEquals(0, store.progress().starsFor(module))
        store.complete("minijuego", stars = 2)
        store.complete("pregunta")
        assertEquals(2, store.progress().starsFor(module))
        assertEquals(2, store.progress().totalStars(listOf(module)))
    }

    @Test
    fun guarda_el_alias_y_el_personaje_elegidos() = withStore { store ->
        store.setAgentProfile("Luna", "mage")
        val progress = store.progress()
        assertEquals("Luna", progress.agentName)
        assertEquals("mage", progress.agentCharacter)
    }

    @Test
    fun reiniciar_borra_todo_para_el_siguiente_jugador() = withStore { store ->
        store.complete("minijuego", stars = 3)
        store.setAgentProfile("Luna", "mage")
        store.markTutorialSeen()

        store.resetAllProgress()

        val progress = store.progress()
        assertEquals(0, progress.totalScore)
        assertTrue(progress.bestStars.isEmpty())
        assertEquals(UserProgress.DEFAULT_AGENT_NAME, progress.agentName)
        assertFalse(store.tutorialSeenFlow.first())
    }
}
