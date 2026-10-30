package com.smarteq.pro.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.di.AppContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val onboardingComplete: Flow<Boolean> = settingsRepository.onboardingComplete

    fun markComplete() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(container.settingsRepository) as T
                }
            }
    }
}
