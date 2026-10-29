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
import kotlinx.coroutines.launch

data class HomeUiState(
    val modules: List<CyberModule> = emptyList(),
    val progress: UserProgress = UserProgress(),
    val totalBadgesCount: Int = 0,
    val unlockedBadgesCount: Int = 0
)

class HomeViewModel(private val repository: CyberRepository) : ViewModel() {

    private val modules = repository.getModules()

    val uiState: StateFlow<HomeUiState> = repository.userProgress
        .combine(kotlinx.coroutines.flow.flowOf(modules)) { progress, moduleList ->
            HomeUiState(
                modules = moduleList,
                progress = progress,
                totalBadgesCount = moduleList.size,
                unlockedBadgesCount = progress.unlockedBadgeIds.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(modules = modules)
        )

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetProgress()
        }
    }

    companion object {
        fun provideFactory(repository: CyberRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
