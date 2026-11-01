package com.example.cybertura.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.cybertura.audio.SoundManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.data.repository.CyberRepository
import com.example.cybertura.game.engine.NpcRegistry
import com.example.cybertura.ui.screens.*
import com.example.cybertura.ui.viewmodel.BadgesViewModel
import com.example.cybertura.ui.viewmodel.GameplayViewModel
import com.example.cybertura.ui.viewmodel.HomeViewModel

@Composable
fun CyberturaNavHost(
    repository: CyberRepository,
    navController: NavHostController = rememberNavController()
) {
    // Detectar si es la primera vez (tutorial no visto). Se lee una sola vez al abrir la app:
    // si cambiara después (al reiniciar el progreso) el NavHost se reconstruiría por su cuenta.
    val tutorialSeen by produceState<Boolean?>(initialValue = null) {
        value = repository.tutorialSeen.first()
    }

    // Mientras carga la preferencia, no renderizar nada para evitar parpadeo
    val startDestination = when (tutorialSeen) {
        null  -> return  // Todavía cargando
        false -> Screen.Tutorial.route
        else  -> Screen.Home.route
    }

    // Volumen del ambiente según la pantalla: protagonista al explorar,
    // bajo en menús y casi inaudible durante los retos para poder concentrarse.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        SoundManager.setAmbientVolume(
            when (currentRoute) {
                Screen.Home.route -> SoundManager.AMBIENT_WORLD
                Screen.Gameplay.route, Screen.ModuleResult.route -> SoundManager.AMBIENT_FOCUS
                Screen.GameCompleted.route -> SoundManager.AMBIENT_MUTED
                else -> SoundManager.AMBIENT_MENU
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Tutorial (solo primera vez) ───────────────────────────────────────
        composable(Screen.Tutorial.route) {
            val scope = rememberCoroutineScope()
            TutorialScreen(
                onTutorialFinished = {
                    scope.launch { repository.markTutorialSeen() }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Tutorial.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Pantalla Principal (Mundo 2D) ─────────────────────────────────────
        composable(Screen.Home.route) {
            val gameWorldViewModel: com.example.cybertura.ui.viewmodel.GameWorldViewModel = viewModel(
                factory = com.example.cybertura.ui.viewmodel.GameWorldViewModel.provideFactory(repository)
            )
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())

            GameWorldScreen(
                viewModel = gameWorldViewModel,
                userProgress = userProgress,
                onNavigateToModule = { moduleId ->
                    navController.navigate(Screen.ModuleDetail.createRoute(moduleId))
                },
                onBadgesClick = {
                    navController.navigate(Screen.Badges.route)
                }
            )
        }

        // ── Pantalla de Detalle de Módulo ─────────────────────────────────────
        composable(
            route = Screen.ModuleDetail.route,
            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
            val module = repository.getModuleById(moduleId)
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())

            if (module != null) {
                ModuleDetailScreen(
                    module = module,
                    userProgress = userProgress,
                    onBackClick = { navController.popBackStack() },
                    onBadgesClick = { navController.navigate(Screen.Badges.route) },
                    onChallengeClick = { challengeId ->
                        navController.navigate(Screen.Gameplay.createRoute(moduleId, challengeId))
                    }
                )
            }
        }

        // ── Pantalla de Juego / Reto Interactivo ──────────────────────────────
        composable(
            route = Screen.Gameplay.route,
            arguments = listOf(
                navArgument("moduleId") { type = NavType.StringType },
                navArgument("challengeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: return@composable

            val gameplayViewModel: GameplayViewModel = viewModel(
                key = "$moduleId-$challengeId",
                factory = GameplayViewModel.provideFactory(repository, moduleId, challengeId)
            )
            val uiState by gameplayViewModel.uiState.collectAsState()

            GameplayScreen(
                uiState = uiState,
                onOptionSelected = { gameplayViewModel.selectOption(it) },
                onDismissFeedback = { gameplayViewModel.dismissFeedback() },
                onNavigateNext = {
                    val nextId = uiState.nextChallengeId
                    if (nextId != null) {
                        navController.navigate(Screen.Gameplay.createRoute(moduleId, nextId)) {
                            popUpTo(Screen.Gameplay.route) { inclusive = true }
                        }
                    }
                },
                onNavigateResult = {
                    navController.navigate(Screen.ModuleResult.createRoute(moduleId)) {
                        popUpTo(Screen.ModuleDetail.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
                onSwipe = { gameplayViewModel.swipeCard(it) },
                onAcknowledgeSwipe = { gameplayViewModel.acknowledgeSwipeMistake() },
                onTapScene = { gameplayViewModel.tapScene(it) },
                onUseHint = { gameplayViewModel.useSpotHint() },
                onChooseChatReply = { gameplayViewModel.chooseChatReply(it) },
                onRetryChatReply = { gameplayViewModel.retryChatReply() },
                onToggleForgeBlock = { gameplayViewModel.toggleForgeBlock(it) },
                onForgeKey = { gameplayViewModel.forgeKey() },
                onChooseForgeOption = { gameplayViewModel.chooseForgeOption(it) },
                onContinueForge = { gameplayViewModel.continueForge() },
                onStartArcadeWave = { gameplayViewModel.startArcadeWave() },
                onArcadeWaveEnded = { lost, failed -> gameplayViewModel.onArcadeWaveEnded(lost, failed) }
            )
        }

        // ── Pantalla de Felicitación / Misión Completada ───────────────────────
        composable(
            route = Screen.ModuleResult.route,
            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
            val module = repository.getModuleById(moduleId)
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())

            ModuleResultScreen(
                module = module,
                onGoHome = {
                    // Si acaban de completar el último módulo de la historia
                    if (userProgress.completedModuleIds.size >= NpcRegistry.questNpcs.size) {
                        navController.navigate(Screen.GameCompleted.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    } else {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                },
                onViewBadges = {
                    navController.navigate(Screen.Badges.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // ── Sala de Medallas / Trofeos ────────────────────────────────────────
        composable(Screen.Badges.route) {
            val badgesViewModel: BadgesViewModel = viewModel(
                factory = BadgesViewModel.provideFactory(repository)
            )
            val uiState by badgesViewModel.uiState.collectAsState()

            BadgesScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
                onResetProgress = {
                    badgesViewModel.resetProgress {
                        // Vaciar toda la pila: el mundo (y su ViewModel) se crea de nuevo
                        // y la aventura arranca otra vez desde el tutorial.
                        navController.navigate(Screen.Tutorial.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Pantalla de Finalización del Juego ──────────────────────────────────
        composable(Screen.GameCompleted.route) {
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())
            GameCompletedScreen(
                userProgress = userProgress,
                onFinish = {
                    // Vuelve a Home y borra el historial hasta allí
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
