package com.womensafety.sos.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.data.pref.SafetySettings
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.SafetyRepository
import com.womensafety.sos.service.GuardianMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefsRepository: UserPreferencesRepository = ServiceLocator.userPreferences,
    private val repository: SafetyRepository = ServiceLocator.repository
) : ViewModel() {

    val settings: StateFlow<SafetySettings> = prefsRepository.settings

    val pairedWards: StateFlow<List<PairedWard>> = repository.getAllPairedWards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setGuardianMonitoring(enabled: Boolean, context: Context) {
        prefsRepository.updateGuardianMonitoring(enabled)
        if (enabled) {
            GuardianMonitorService.start(context)
        } else {
            GuardianMonitorService.stop(context)
        }
    }

    fun updateDisplayName(name: String) {
        prefsRepository.updateDisplayName(name)
    }

    fun addPairedWard(pairingCode: String, wardName: String) {
        viewModelScope.launch {
            repository.addPairedWard(pairingCode, wardName)
        }
    }

    fun deletePairedWard(id: Long) {
        viewModelScope.launch {
            repository.deletePairedWard(id)
        }
    }
}
