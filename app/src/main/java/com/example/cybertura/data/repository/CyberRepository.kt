package com.example.cybertura.data.repository

import com.example.cybertura.data.local.DataStoreManager
import com.example.cybertura.data.local.JsonDataLoader
import com.example.cybertura.data.model.Challenge
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    suspend fun completeChallenge(moduleId: String, challengeId: String, points: Int) {
        val currentProgress = userProgress.first()
        val module = getModuleById(moduleId) ?: return

        val alreadyCompleted = currentProgress.completedChallengeIds.contains(challengeId)
        val allModuleChallenges = module.challenges.map { it.id }.toSet()
        
        // Check if completing this challenge makes all challenges in the module completed
        val willBeCompleted = currentProgress.completedChallengeIds + challengeId
        val isModuleFinished = allModuleChallenges.isNotEmpty() && willBeCompleted.containsAll(allModuleChallenges)
        val badgeToUnlock = if (isModuleFinished) module.badgeId else null

        dataStoreManager.recordChallengeSuccess(
            challengeId = challengeId,
            pointsAwarded = if (alreadyCompleted) 0 else points,
            moduleId = moduleId,
            isModuleCompletedNow = isModuleFinished,
            badgeIdToUnlock = badgeToUnlock
        )
    }

    suspend fun resetProgress() {
        dataStoreManager.resetAllProgress()
    }
}
