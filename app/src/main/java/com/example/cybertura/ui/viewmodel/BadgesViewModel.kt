package com.example.cybertura.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cybertura.data.model.CyberModule
import com.example.cybertura.data.model.UserProgress
import com.example.cybertura.data.repository.CyberRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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

class BadgesViewModel(repository: CyberRepository) : ViewModel() {

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

    companion object {
        fun provideFactory(repository: CyberRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BadgesViewModel(repository) as T
                }
            }
    }
}
