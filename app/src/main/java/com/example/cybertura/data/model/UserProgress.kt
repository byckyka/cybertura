package com.example.cybertura.data.model

data class UserProgress(
    val completedChallengeIds: Set<String> = emptySet(),
    val completedModuleIds: Set<String> = emptySet(),
    val unlockedBadgeIds: Set<String> = emptySet(),
    val totalScore: Int = 0,
    val agentName: String = "Ciber-Detective"
) {
    // ─── RPG Level System ─────────────────────────────────────
    companion object {
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
}
