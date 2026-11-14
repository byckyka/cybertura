package io.github.byckyka.cybertura.data.local

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
import io.github.byckyka.cybertura.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cybertura_progress")

/**
 * Progreso del jugador guardado en el teléfono.
 *
 * Recibe el [DataStore] ya creado para poder probarse sin Android; la app usa el constructor
 * que lo saca del [Context].
 */
class DataStoreManager(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    companion object {
        private val KEY_COMPLETED_CHALLENGES = stringSetPreferencesKey("completed_challenges")
        private val KEY_COMPLETED_MODULES = stringSetPreferencesKey("completed_modules")
        private val KEY_UNLOCKED_BADGES = stringSetPreferencesKey("unlocked_badges")
        private val KEY_TOTAL_SCORE = intPreferencesKey("total_score")
        private val KEY_AGENT_NAME = stringPreferencesKey("agent_name")
        private val KEY_AGENT_CHARACTER = stringPreferencesKey("agent_character")
        private val KEY_TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        private val KEY_GAME_COMPLETED_SEEN = booleanPreferencesKey("game_completed_seen")

        /** Mejor marca de estrellas de cada reto: una clave por reto, `stars_<id>`. */
        private const val STARS_PREFIX = "stars_"
        private fun starsKey(challengeId: String) = intPreferencesKey(STARS_PREFIX + challengeId)
    }

    /** Lo guardado; si el archivo no se puede leer se trata como vacío en vez de cerrar la app. */
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val gameCompletedSeenFlow: Flow<Boolean> = preferences.map { it[KEY_GAME_COMPLETED_SEEN] ?: false }

    suspend fun markGameCompletedSeen() {
        dataStore.edit { it[KEY_GAME_COMPLETED_SEEN] = true }
    }

    val tutorialSeenFlow: Flow<Boolean> = preferences.map { it[KEY_TUTORIAL_SEEN] ?: false }

    suspend fun markTutorialSeen() {
        dataStore.edit { it[KEY_TUTORIAL_SEEN] = true }
    }

    val userProgressFlow: Flow<UserProgress> = preferences.map { prefs ->
        UserProgress(
            completedChallengeIds = prefs[KEY_COMPLETED_CHALLENGES] ?: emptySet(),
            completedModuleIds = prefs[KEY_COMPLETED_MODULES] ?: emptySet(),
            unlockedBadgeIds = prefs[KEY_UNLOCKED_BADGES] ?: emptySet(),
            totalScore = prefs[KEY_TOTAL_SCORE] ?: 0,
            agentName = prefs[KEY_AGENT_NAME] ?: UserProgress.DEFAULT_AGENT_NAME,
            agentCharacter = prefs[KEY_AGENT_CHARACTER] ?: "",
            bestStars = prefs.asMap().mapNotNull { (key, value) ->
                if (key.name.startsWith(STARS_PREFIX) && value is Int) {
                    key.name.removePrefix(STARS_PREFIX) to value
                } else {
                    null
                }
            }.toMap()
        )
    }

    /**
     * Registra un reto completado. Todo se decide y se guarda dentro de una sola edición de
     * DataStore, de modo que dos llamadas seguidas no pueden pisarse: los puntos se suman solo
     * la primera vez, la medalla se otorga cuando están hechos todos los retos del módulo y de
     * las estrellas se conserva la mejor marca.
     *
     * @param stars estrellas del intento (1 a 3), o null si el reto no da estrellas.
     */
    suspend fun recordChallengeSuccess(
        challengeId: String,
        points: Int,
        moduleId: String,
        moduleChallengeIds: Set<String>,
        badgeId: String,
        stars: Int? = null
    ) {
        dataStore.edit { preferences ->
            val previous = preferences[KEY_COMPLETED_CHALLENGES].orEmpty()
            val challenges = previous + challengeId
            preferences[KEY_COMPLETED_CHALLENGES] = challenges

            if (challengeId !in previous) {
                preferences[KEY_TOTAL_SCORE] = (preferences[KEY_TOTAL_SCORE] ?: 0) + points
            }

            if (stars != null) {
                val key = starsKey(challengeId)
                preferences[key] = maxOf(preferences[key] ?: 0, stars.coerceIn(1, UserProgress.MAX_STARS))
            }

            if (moduleChallengeIds.isNotEmpty() && challenges.containsAll(moduleChallengeIds)) {
                preferences[KEY_COMPLETED_MODULES] = preferences[KEY_COMPLETED_MODULES].orEmpty() + moduleId
                if (badgeId.isNotBlank()) {
                    preferences[KEY_UNLOCKED_BADGES] = preferences[KEY_UNLOCKED_BADGES].orEmpty() + badgeId
                }
            }
        }
    }

    /** Guarda el alias y el personaje que el jugador eligió al terminar el tutorial. */
    suspend fun setAgentProfile(name: String, character: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AGENT_NAME] = name
            preferences[KEY_AGENT_CHARACTER] = character
        }
    }

    suspend fun resetAllProgress() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
