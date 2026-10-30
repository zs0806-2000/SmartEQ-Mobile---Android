package com.smarteq.pro.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarteq.pro.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val onboardingComplete: Flow<Boolean> = settingsRepository.onboardingComplete

    fun markComplete() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }
}
