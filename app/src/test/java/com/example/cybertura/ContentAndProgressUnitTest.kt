package com.example.cybertura

import com.example.cybertura.data.model.Challenge
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.Option
import com.example.cybertura.data.model.UserProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentAndProgressUnitTest {

    @Test
    fun userProgress_calculatesCompletionCorrectly() {
        val challenges = listOf(
            Challenge(id = "c1", title = "Reto 1", question = "¿Pregunta 1?", points = 100),
            Challenge(id = "c2", title = "Reto 2", question = "¿Pregunta 2?", points = 100)
        )
        val module = CyberModule(
            id = "mod_test",
            title = "Módulo Prueba",
            challenges = challenges
        )

        // Estado inicial
        val initialProgress = UserProgress()
        assertFalse(initialProgress.isChallengeCompleted("c1"))
        assertEquals(0f, initialProgress.getModuleProgressPercentage(module), 0.01f)
        assertEquals(0, initialProgress.getCompletedChallengesCount(module))

        // Reto 1 completado
        val halfwayProgress = initialProgress.copy(
            completedChallengeIds = setOf("c1")
        )
        assertTrue(halfwayProgress.isChallengeCompleted("c1"))
        assertFalse(halfwayProgress.isChallengeCompleted("c2"))
        assertEquals(0.5f, halfwayProgress.getModuleProgressPercentage(module), 0.01f)
        assertEquals(1, halfwayProgress.getCompletedChallengesCount(module))

        // Todos los retos completados
        val fullProgress = halfwayProgress.copy(
            completedChallengeIds = setOf("c1", "c2"),
            completedModuleIds = setOf("mod_test")
        )
        assertTrue(fullProgress.isChallengeCompleted("c1"))
        assertTrue(fullProgress.isChallengeCompleted("c2"))
        assertTrue(fullProgress.isModuleCompleted("mod_test"))
        assertEquals(1.0f, fullProgress.getModuleProgressPercentage(module), 0.01f)
        assertEquals(2, fullProgress.getCompletedChallengesCount(module))
    }

    @Test
    fun challenge_validatesOptionsIntegrity() {
        val options = listOf(
            Option(id = "1", text = "Opción A", isCorrect = false, feedback = "Incorrecto"),
            Option(id = "2", text = "Opción B", isCorrect = true, feedback = "¡Correcto!"),
            Option(id = "3", text = "Opción C", isCorrect = false, feedback = "Incorrecto")
        )
        val challenge = Challenge(
            id = "pwd_test",
            title = "¿Contraseña segura?",
            question = "¿Cuál es segura?",
            points = 100,
            educationalTip = "Consejo de seguridad",
            options = options
        )

        val correctCount = challenge.options.count { it.isCorrect }
        assertEquals(1, correctCount)
        assertTrue(challenge.educationalTip.isNotBlank())
    }

    // ─── RPG Level System Tests ───────────────────────────────
    @Test
    fun userProgress_levelSystemCalculatesCorrectly() {
        // Level 1 at start (0 points)
        val newPlayer = UserProgress(totalScore = 0)
        assertEquals(1, newPlayer.level)
        assertEquals(0, newPlayer.currentXp)
        assertEquals(200, newPlayer.xpToNextLevel)
        assertEquals(0f, newPlayer.xpProgress, 0.01f)
        assertEquals("Novato Digital", newPlayer.rankTitle)

        // Halfway through level 1 (100 points)
        val midLevel1 = UserProgress(totalScore = 100)
        assertEquals(1, midLevel1.level)
        assertEquals(100, midLevel1.currentXp)
        assertEquals(0.5f, midLevel1.xpProgress, 0.01f)

        // Level 2 (200 points)
        val level2 = UserProgress(totalScore = 200)
        assertEquals(2, level2.level)
        assertEquals(0, level2.currentXp)
        assertEquals("Aprendiz Cyber", level2.rankTitle)

        // Level 3 (450 points)
        val level3 = UserProgress(totalScore = 450)
        assertEquals(3, level3.level)
        assertEquals(50, level3.currentXp)
        assertEquals("Explorador de Red", level3.rankTitle)

        // High level (1600 points = level 9, capped at last rank)
        val highLevel = UserProgress(totalScore = 1600)
        assertEquals(9, highLevel.level)
        assertEquals("Campeón Legendario", highLevel.rankTitle)
    }

    @Test
    fun userProgress_xpProgressIsAlwaysBetweenZeroAndOne() {
        for (score in listOf(0, 50, 100, 150, 199, 200, 201, 500, 999)) {
            val progress = UserProgress(totalScore = score)
            assertTrue(
                "xpProgress for score=$score should be in [0,1] but was ${progress.xpProgress}",
                progress.xpProgress in 0f..1f
            )
        }
    }
}
