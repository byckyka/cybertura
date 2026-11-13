package com.example.cybertura.ui.navigation

sealed class Screen(val route: String) {
    object Tutorial : Screen("tutorial")
    /** Alias y personaje del jugador, justo después del tutorial. */
    object Profile : Screen("profile")
    object Home : Screen("home")
    object Badges : Screen("badges")
    object GameCompleted : Screen("game_completed")
    object Credits : Screen("credits")

    object ModuleDetail : Screen("module/{moduleId}") {
        fun createRoute(moduleId: String) = "module/$moduleId"
    }

    /** Mini lección del módulo; al terminarla se abre el reto [challengeId]. */
    object Lesson : Screen("lesson/{moduleId}/{challengeId}") {
        fun createRoute(moduleId: String, challengeId: String) = "lesson/$moduleId/$challengeId"
    }

    object Gameplay : Screen("gameplay/{moduleId}/{challengeId}") {
        fun createRoute(moduleId: String, challengeId: String) = "gameplay/$moduleId/$challengeId"
    }

    object ModuleResult : Screen("module_result/{moduleId}") {
        fun createRoute(moduleId: String) = "module_result/$moduleId"
    }
}
