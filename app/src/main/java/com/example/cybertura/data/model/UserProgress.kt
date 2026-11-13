package com.example.cybertura.data.model

data class UserProgress(
    val completedChallengeIds: Set<String> = emptySet(),
    val completedModuleIds: Set<String> = emptySet(),
    val unlockedBadgeIds: Set<String> = emptySet(),
    val totalScore: Int = 0,
    /** Alias que el jugador eligió; nunca su nombre real. */
    val agentName: String = DEFAULT_AGENT_NAME,
    /** Personaje elegido (id de `PlayerCharacters`); vacío = el personaje por defecto. */
    val agentCharacter: String = "",
    /** Mejor marca de estrellas (1 a 3) de cada reto que las da, por id de reto. */
    val bestStars: Map<String, Int> = emptyMap()
) {
    // ─── RPG Level System ─────────────────────────────────────
    companion object {
        const val DEFAULT_AGENT_NAME = "Ciber-Detective"
        const val MAX_STARS = 3
        const val XP_PER_LEVEL = 200
        val RANK_TITLES = listOf(
            "Novato Digital",      // Lvl 1
            "Aprendiz Cyber",      // Lvl 2
            "Explorador de Red",   // Lvl 3
            "Centinela Digital",   // Lvl 4
            "Guardián Cibernético",// Lvl 5
            "Caballero del Firewall", // Lvl 6
            "Maestro del Código",  // Lvl 7
            "Campeón Legendario"   // Lvl 8+
        )
    }

    val level: Int
        get() = (totalScore / XP_PER_LEVEL) + 1

    val currentXp: Int
        get() = totalScore % XP_PER_LEVEL

    val xpToNextLevel: Int
        get() = XP_PER_LEVEL

    val xpProgress: Float
        get() = currentXp.toFloat() / XP_PER_LEVEL.toFloat()

    val rankTitle: String
        get() = RANK_TITLES.getOrElse((level - 1).coerceIn(0, RANK_TITLES.lastIndex)) {
            RANK_TITLES.last()
        }

    // ─── Existing helpers ─────────────────────────────────────
    fun isChallengeCompleted(challengeId: String): Boolean {
        return completedChallengeIds.contains(challengeId)
    }

    fun isModuleCompleted(moduleId: String): Boolean {
        return completedModuleIds.contains(moduleId)
    }

    fun getModuleProgressPercentage(module: CyberModule): Float {
        if (module.challenges.isEmpty()) return 0f
        val completedCount = module.challenges.count { completedChallengeIds.contains(it.id) }
        return completedCount.toFloat() / module.challenges.size.toFloat()
    }

    fun getCompletedChallengesCount(module: CyberModule): Int {
        return module.challenges.count { completedChallengeIds.contains(it.id) }
    }

    /** Mejor marca de un reto; 0 si aún no tiene estrellas. */
    fun starsFor(challengeId: String): Int = bestStars[challengeId] ?: 0

    /**
     * Estrellas del módulo: las de su minijuego (la pregunta final no da estrellas).
     * 0 si todavía no lo ha completado.
     */
    fun starsFor(module: CyberModule): Int =
        module.challenges.maxOfOrNull { starsFor(it.id) } ?: 0

    /** Suma de las estrellas de todos los módulos, para el contador de la sala de trofeos. */
    fun totalStars(modules: List<CyberModule>): Int = modules.sumOf { starsFor(it) }
}
