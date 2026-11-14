package io.github.byckyka.cybertura.data.repository

import io.github.byckyka.cybertura.data.local.DataStoreManager
import io.github.byckyka.cybertura.data.local.JsonDataLoader
import io.github.byckyka.cybertura.data.model.Challenge
import io.github.byckyka.cybertura.data.model.CyberModule
import io.github.byckyka.cybertura.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

class CyberRepository(
    private val jsonDataLoader: JsonDataLoader,
    private val dataStoreManager: DataStoreManager
) {
    // Cache modules in memory after loading
    private val cachedModules: List<CyberModule> by lazy {
        jsonDataLoader.loadEducationalContent().modules
    }

    val userProgress: Flow<UserProgress> = dataStoreManager.userProgressFlow
    val tutorialSeen: Flow<Boolean> = dataStoreManager.tutorialSeenFlow

    suspend fun markTutorialSeen() = dataStoreManager.markTutorialSeen()

    /** true cuando el jugador ya vio la pantalla final; así no se repite al rejugar un módulo. */
    val gameCompletedSeen: Flow<Boolean> = dataStoreManager.gameCompletedSeenFlow

    suspend fun markGameCompletedSeen() = dataStoreManager.markGameCompletedSeen()

    fun getModules(): List<CyberModule> = cachedModules

    fun getModuleById(moduleId: String): CyberModule? {
        return cachedModules.firstOrNull { it.id == moduleId }
    }

    fun getChallenge(moduleId: String, challengeId: String): Challenge? {
        return getModuleById(moduleId)?.challenges?.firstOrNull { it.id == challengeId }
    }

    /** @param stars estrellas del intento (1 a 3); null si el reto no da estrellas. */
    suspend fun completeChallenge(moduleId: String, challengeId: String, points: Int, stars: Int? = null) {
        val module = getModuleById(moduleId) ?: return
        dataStoreManager.recordChallengeSuccess(
            challengeId = challengeId,
            points = points,
            moduleId = moduleId,
            moduleChallengeIds = module.challenges.map { it.id }.toSet(),
            badgeId = module.badgeId,
            stars = stars
        )
    }

    /** Alias y personaje elegidos al terminar el tutorial. */
    suspend fun setAgentProfile(name: String, character: String) =
        dataStoreManager.setAgentProfile(name, character)

    suspend fun resetProgress() {
        dataStoreManager.resetAllProgress()
    }
}
