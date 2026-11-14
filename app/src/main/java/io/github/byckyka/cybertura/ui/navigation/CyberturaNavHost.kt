package io.github.byckyka.cybertura.ui.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.byckyka.cybertura.research.ResearchLogger
import io.github.byckyka.cybertura.research.TestMoment
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.byckyka.cybertura.audio.SoundManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import io.github.byckyka.cybertura.data.model.UserProgress
import io.github.byckyka.cybertura.data.repository.CyberRepository
import io.github.byckyka.cybertura.game.engine.NpcRegistry
import io.github.byckyka.cybertura.ui.screens.*
import io.github.byckyka.cybertura.ui.viewmodel.BadgesViewModel
import io.github.byckyka.cybertura.ui.viewmodel.GameplayViewModel

@Composable
fun CyberturaNavHost(
    repository: CyberRepository,
    researchLogger: ResearchLogger? = null,
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
            TutorialScreen(
                onTutorialFinished = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Tutorial.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Alias y personaje (cierra la bienvenida) ──────────────────────────
        composable(Screen.Profile.route) {
            val scope = rememberCoroutineScope()
            ProfileSetupScreen(
                onDone = { name, characterId ->
                    scope.launch {
                        // Se guarda antes de entrar al mundo para que ya se vea el personaje elegido.
                        // El tutorial se marca como visto aquí: si la app se cierra antes, se repite
                        // la bienvenida completa en vez de entrar sin perfil.
                        repository.setAgentProfile(name, characterId)
                        repository.markTutorialSeen()
                        // En el piloto, antes de jugar se responde el cuestionario de conocimientos
                        val status = researchLogger?.status?.value
                        val next = if (status != null && status.isRecording && !status.preTestDone) {
                            Screen.KnowledgeTest.createRoute(TestMoment.PRE.code)
                        } else {
                            Screen.Home.route
                        }
                        navController.navigate(next) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Pantalla Principal (Mundo 2D) ─────────────────────────────────────
        composable(Screen.Home.route) {
            val gameWorldViewModel: io.github.byckyka.cybertura.ui.viewmodel.GameWorldViewModel = viewModel(
                factory = io.github.byckyka.cybertura.ui.viewmodel.GameWorldViewModel.provideFactory(repository)
            )
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())

            // En el piloto nadie empieza a jugar sin el cuestionario inicial: si la app se cerró
            // antes de responderlo, al volver al mundo se abre otra vez.
            val researchStatus = researchLogger?.status?.collectAsState()?.value
            val needsPreTest = researchStatus != null && researchStatus.isRecording && !researchStatus.preTestDone
            LaunchedEffect(needsPreTest) {
                if (needsPreTest) {
                    navController.navigate(Screen.KnowledgeTest.createRoute(TestMoment.PRE.code)) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }

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
                        // El primer reto del módulo (su minijuego) va precedido de la mini lección
                        val isFirst = module.challenges.firstOrNull()?.id == challengeId
                        val hasLesson = !module.lesson?.cards.isNullOrEmpty()
                        navController.navigate(
                            if (isFirst && hasLesson) {
                                Screen.Lesson.createRoute(moduleId, challengeId)
                            } else {
                                Screen.Gameplay.createRoute(moduleId, challengeId)
                            }
                        )
                    }
                )
            }
        }

        // ── Mini lección antes del minijuego ──────────────────────────────────
        composable(
            route = Screen.Lesson.route,
            arguments = listOf(
                navArgument("moduleId") { type = NavType.StringType },
                navArgument("challengeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: return@composable
            val module = repository.getModuleById(moduleId) ?: return@composable
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())

            LessonScreen(
                module = module,
                canSkip = userProgress.isChallengeCompleted(challengeId),
                onStartChallenge = {
                    // La lección sale de la pila: "atrás" desde el reto vuelve al módulo
                    navController.navigate(Screen.Gameplay.createRoute(moduleId, challengeId)) {
                        popUpTo(Screen.Lesson.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
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
                factory = GameplayViewModel.provideFactory(repository, moduleId, challengeId, researchLogger)
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
                onArcadeWaveEnded = { gameplayViewModel.onArcadeWaveEnded() },
                onArcadeItemResolved = { item, tapped -> gameplayViewModel.onArcadeItemResolved(item, tapped) }
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
            // Mientras carga se asume "ya vista" para no repetir la pantalla final por error
            val gameCompletedSeen by repository.gameCompletedSeen.collectAsState(initial = true)

            ModuleResultScreen(
                module = module,
                onGoHome = {
                    // Solo la primera vez que se completa el último módulo de la historia;
                    // al rejugar un módulo después se vuelve directo al mundo.
                    val allModulesDone = userProgress.completedModuleIds.size >= NpcRegistry.questNpcs.size
                    if (allModulesDone && !gameCompletedSeen) {
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
                factory = BadgesViewModel.provideFactory(repository, researchLogger)
            )
            val uiState by badgesViewModel.uiState.collectAsState()
            val researchStatus by badgesViewModel.researchStatus.collectAsState()
            val context = LocalContext.current
            // El investigador elige una carpeta (ej. Descargas) y ahí se guardan los CSV
            val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    badgesViewModel.exportToFolder(context.contentResolver, uri) { ok ->
                        val msg = if (ok) "Se guardó el archivo de resultados (CSV)" else "No se pudo guardar el archivo"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
            val restartAdventure = {
                // Vaciar toda la pila: el mundo (y su ViewModel) se crea de nuevo
                // y la aventura arranca otra vez desde el tutorial.
                navController.navigate(Screen.Tutorial.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }

            BadgesScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
                onCreditsClick = { navController.navigate(Screen.Credits.route) },
                onResetProgress = { badgesViewModel.resetProgress(restartAdventure) },
                researchStatus = researchStatus,
                onStartResearchSession = { code -> badgesViewModel.startResearchSession(code, restartAdventure) },
                onEndResearchSession = { badgesViewModel.endResearchSession() },
                onExportResearchToFolder = { folderPicker.launch(null) },
                onShareResearch = { badgesViewModel.buildShareIntent { context.startActivity(it) } },
                onClearResearch = {
                    badgesViewModel.clearResearchData()
                    Toast.makeText(context, "Datos del piloto borrados", Toast.LENGTH_SHORT).show()
                },
                onOpenSurvey = { navController.navigate(Screen.Survey.route) },
                onOpenFinalTest = {
                    navController.navigate(Screen.KnowledgeTest.createRoute(TestMoment.POST.code))
                }
            )
        }

        // ── Pantalla de Finalización del Juego ──────────────────────────────────
        composable(Screen.GameCompleted.route) {
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())
            LaunchedEffect(Unit) { repository.markGameCompletedSeen() }
            GameCompletedScreen(
                userProgress = userProgress,
                onShowCredits = { navController.navigate(Screen.Credits.route) },
                onFinish = { finishAdventure(navController, researchLogger) }
            )
        }

        // ── Cuestionario de conocimientos, antes y después (modo investigador) ─
        composable(
            route = Screen.KnowledgeTest.route,
            arguments = listOf(navArgument("moment") { type = NavType.StringType })
        ) { backStackEntry ->
            val moment = TestMoment.fromCode(backStackEntry.arguments?.getString("moment")) ?: return@composable
            KnowledgeTestScreen(
                moment = moment,
                onSubmit = { answers ->
                    researchLogger?.saveKnowledgeTest(moment, answers)
                    when {
                        // Antes de jugar: el mundo aún no está en la pila
                        moment == TestMoment.PRE -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.KnowledgeTest.route) { inclusive = true }
                        }
                        // Después de jugar sigue la encuesta de percepción, si falta
                        researchLogger?.status?.value?.surveyDone == false ->
                            navController.navigate(Screen.Survey.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        else -> navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                }
            )
        }

        // ── Encuesta de percepción (modo investigador) ────────────────────────
        composable(Screen.Survey.route) {
            val context = LocalContext.current
            SurveyScreen(
                onSubmit = { answers ->
                    researchLogger?.saveSurvey(answers)
                    Toast.makeText(context, "¡Gracias por tus respuestas!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        // ── Créditos (al terminar la aventura o desde la Sala de Trofeos) ───────
        composable(Screen.Credits.route) {
            val userProgress by repository.userProgress.collectAsState(initial = UserProgress())
            // Desde los trofeos se regresa a ellos; desde el final del juego, al mundo
            val fromBadges = remember {
                navController.previousBackStackEntry?.destination?.route == Screen.Badges.route
            }
            CreditsScreen(
                agentName = userProgress.agentName,
                finishLabel = if (fromBadges) "Volver" else "Volver al Mundo",
                onFinish = {
                    if (fromBadges) {
                        navController.popBackStack()
                    } else {
                        // Desde el final del juego: mismo cierre que "Volver al Mundo"
                        finishAdventure(navController, researchLogger)
                    }
                }
            )
        }
    }
}

/**
 * Cierra la aventura al salir de la pantalla final o de sus créditos. En el piloto (modo
 * investigador grabando) lleva primero al cuestionario de después y a la encuesta si faltan;
 * si no, vuelve al mundo y borra el historial hasta allí.
 */
private fun finishAdventure(navController: NavHostController, researchLogger: ResearchLogger?) {
    val status = researchLogger?.status?.value
    val pilotStep = when {
        status == null || !status.isRecording -> null
        !status.postTestDone -> Screen.KnowledgeTest.createRoute(TestMoment.POST.code)
        !status.surveyDone -> Screen.Survey.route
        else -> null
    }
    if (pilotStep != null) {
        navController.navigate(pilotStep) {
            popUpTo(Screen.Home.route) { inclusive = false }
        }
    } else {
        navController.popBackStack(Screen.Home.route, inclusive = false)
    }
}
