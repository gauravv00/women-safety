package com.womensafety.sos.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.womensafety.sos.data.pref.SafetySettings
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.di.ServiceLocator
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val prefsRepository: UserPreferencesRepository = ServiceLocator.userPreferences
) : ViewModel() {

    val settings: StateFlow<SafetySettings> = prefsRepository.settings

    fun setShakeToTrigger(enabled: Boolean) {
        prefsRepository.updateShakeToTrigger(enabled)
    }

    fun setLoudSiren(enabled: Boolean) {
        prefsRepository.updateLoudSiren(enabled)
    }

    fun setAutoCallTopContact(enabled: Boolean) {
        prefsRepository.updateAutoCallTopContact(enabled)
    }

    fun setAutoCallDelay(seconds: Int) {
        prefsRepository.updateAutoCallDelay(seconds)
    }

    fun setSequentialEscalation(enabled: Boolean) {
        prefsRepository.updateSequentialEscalation(enabled)
    }

    fun setBatteryAware(enabled: Boolean) {
        prefsRepository.updateBatteryAware(enabled)
    }
}
