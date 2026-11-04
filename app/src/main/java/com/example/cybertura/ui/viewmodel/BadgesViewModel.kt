package com.example.cybertura.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.data.repository.CyberRepository
import com.example.cybertura.research.ResearchLogger
import com.example.cybertura.research.ResearchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val moduleTitle: String,
    val accentColor: String,
    val isUnlocked: Boolean
)

data class BadgesUiState(
    val badges: List<BadgeItem> = emptyList(),
    val totalScore: Int = 0,
    val unlockedCount: Int = 0
)

class BadgesViewModel(
    private val repository: CyberRepository,
    private val researchLogger: ResearchLogger? = null
) : ViewModel() {

    private val modules = repository.getModules()

    val uiState: StateFlow<BadgesUiState> = repository.userProgress
        .combine(kotlinx.coroutines.flow.flowOf(modules)) { progress, moduleList ->
            val badgeItems = moduleList.map { module ->
                BadgeItem(
                    id = module.badgeId,
                    title = module.badgeTitle,
                    description = module.badgeDescription,
                    moduleTitle = module.title,
                    accentColor = module.accentColor,
                    isUnlocked = progress.unlockedBadgeIds.contains(module.badgeId)
                )
            }
            BadgesUiState(
                badges = badgeItems,
                totalScore = progress.totalScore,
                unlockedCount = progress.unlockedBadgeIds.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BadgesUiState()
        )

    /** Borra todo el progreso guardado y avisa cuando termina, para poder navegar después. */
    fun resetProgress(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.resetProgress()
            onDone()
        }
    }

    // ── Modo investigador ────────────────────────────────────────────────────

    val researchStatus: StateFlow<ResearchStatus> =
        researchLogger?.status ?: MutableStateFlow(ResearchStatus())

    /** Cierra la sesión anterior (si había), abre la del nuevo participante y reinicia la aventura. */
    fun startResearchSession(code: String, onDone: () -> Unit) {
        val logger = researchLogger ?: return
        viewModelScope.launch {
            logger.startSession(code)
            repository.resetProgress()
            onDone()
        }
    }

    fun endResearchSession() {
        val logger = researchLogger ?: return
        logger.endSession()
    }

    /** Guarda el CSV de resultados en la carpeta elegida; [onResult] dice si se pudo. */
    fun exportToFolder(resolver: ContentResolver, treeUri: Uri, onResult: (Boolean) -> Unit) {
        val logger = researchLogger ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { logger.exportToFolder(resolver, treeUri) }
            onResult(saved)
        }
    }

    fun buildShareIntent(onReady: (Intent) -> Unit) {
        val logger = researchLogger ?: return
        viewModelScope.launch {
            val intent = withContext(Dispatchers.IO) { logger.shareIntent() }
            onReady(intent)
        }
    }

    fun clearResearchData() {
        val logger = researchLogger ?: return
        viewModelScope.launch(Dispatchers.IO) { logger.clearAll() }
    }

    companion object {
        fun provideFactory(
            repository: CyberRepository,
            researchLogger: ResearchLogger? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BadgesViewModel(repository, researchLogger) as T
                }
            }
    }
}
