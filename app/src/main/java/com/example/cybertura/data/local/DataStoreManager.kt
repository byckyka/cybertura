package com.example.cybertura.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cybertura.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cybertura_progress")

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_COMPLETED_CHALLENGES = stringSetPreferencesKey("completed_challenges")
        private val KEY_COMPLETED_MODULES = stringSetPreferencesKey("completed_modules")
        private val KEY_UNLOCKED_BADGES = stringSetPreferencesKey("unlocked_badges")
        private val KEY_TOTAL_SCORE = intPreferencesKey("total_score")
        private val KEY_AGENT_NAME = stringPreferencesKey("agent_name")
        private val KEY_TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        private val KEY_GAME_COMPLETED_SEEN = booleanPreferencesKey("game_completed_seen")
    }

    val gameCompletedSeenFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_GAME_COMPLETED_SEEN] ?: false }

    suspend fun markGameCompletedSeen() {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAME_COMPLETED_SEEN] = true
        }
    }

    val tutorialSeenFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_TUTORIAL_SEEN] ?: false }

    suspend fun markTutorialSeen() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TUTORIAL_SEEN] = true
        }
    }

    val userProgressFlow: Flow<UserProgress> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserProgress(
                completedChallengeIds = preferences[KEY_COMPLETED_CHALLENGES] ?: emptySet(),
                completedModuleIds = preferences[KEY_COMPLETED_MODULES] ?: emptySet(),
                unlockedBadgeIds = preferences[KEY_UNLOCKED_BADGES] ?: emptySet(),
                totalScore = preferences[KEY_TOTAL_SCORE] ?: 0,
                agentName = preferences[KEY_AGENT_NAME] ?: "Ciber-Detective"
            )
        }

    suspend fun recordChallengeSuccess(
        challengeId: String,
        pointsAwarded: Int,
        moduleId: String? = null,
        isModuleCompletedNow: Boolean = false,
        badgeIdToUnlock: String? = null
    ) {
        context.dataStore.edit { preferences ->
            val currentChallenges = preferences[KEY_COMPLETED_CHALLENGES]?.toMutableSet() ?: mutableSetOf()
            val wasAlreadyCompleted = currentChallenges.contains(challengeId)

            currentChallenges.add(challengeId)
            preferences[KEY_COMPLETED_CHALLENGES] = currentChallenges

            if (!wasAlreadyCompleted) {
                val currentScore = preferences[KEY_TOTAL_SCORE] ?: 0
                preferences[KEY_TOTAL_SCORE] = currentScore + pointsAwarded
            }

            if (isModuleCompletedNow && moduleId != null) {
                val currentModules = preferences[KEY_COMPLETED_MODULES]?.toMutableSet() ?: mutableSetOf()
                currentModules.add(moduleId)
                preferences[KEY_COMPLETED_MODULES] = currentModules
            }

            if (!badgeIdToUnlock.isNullOrBlank()) {
                val currentBadges = preferences[KEY_UNLOCKED_BADGES]?.toMutableSet() ?: mutableSetOf()
                currentBadges.add(badgeIdToUnlock)
                preferences[KEY_UNLOCKED_BADGES] = currentBadges
            }
        }
    }

    suspend fun setAgentName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AGENT_NAME] = name
        }
    }

    suspend fun resetAllProgress() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
