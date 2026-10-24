package com.womensafety.sos.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.data.pref.SafetySettings
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.data.util.PhoneUtils
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.NotificationRepository
import com.womensafety.sos.domain.repository.SafetyRepository
import com.womensafety.sos.service.GuardianMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PairRequestState {
    data object Idle : PairRequestState
    data object Loading : PairRequestState
    data class Success(val message: String) : PairRequestState
    data class Error(val message: String) : PairRequestState
}

class SettingsViewModel(
    private val prefsRepository: UserPreferencesRepository = ServiceLocator.userPreferences,
    private val repository: SafetyRepository = ServiceLocator.repository,
    private val notificationRepository: NotificationRepository = ServiceLocator.notificationRepository
) : ViewModel() {

    val settings: StateFlow<SafetySettings> = prefsRepository.settings

    val pairedWards: StateFlow<List<PairedWard>> = repository.getAllPairedWards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pairRequestState = MutableStateFlow<PairRequestState>(PairRequestState.Idle)
    val pairRequestState: StateFlow<PairRequestState> = _pairRequestState.asStateFlow()

    fun setShakeToTrigger(enabled: Boolean) { prefsRepository.updateShakeToTrigger(enabled) }

    fun setLoudSiren(enabled: Boolean) { prefsRepository.updateLoudSiren(enabled) }

    fun setAutoCallTopContact(enabled: Boolean) { prefsRepository.updateAutoCallTopContact(enabled) }

    fun setAutoCallDelay(seconds: Int) { prefsRepository.updateAutoCallDelay(seconds) }

    fun setSequentialEscalation(enabled: Boolean) { prefsRepository.updateSequentialEscalation(enabled) }

    fun setBatteryAware(enabled: Boolean) { prefsRepository.updateBatteryAware(enabled) }

    fun setGuardianMonitoring(enabled: Boolean, context: Context) {
        prefsRepository.updateGuardianMonitoring(enabled)
        if (enabled) GuardianMonitorService.start(context) else GuardianMonitorService.stop(context)
    }

    fun updateDisplayName(name: String) { prefsRepository.updateDisplayName(name) }

    /** Sends a Firestore pair request to the ward identified by [pairingCode].
     *  Also stores the ward locally on success for GuardianMonitorService. */
    fun sendPairRequest(pairingCode: String, wardName: String) {
        val normalized = PhoneUtils.normalizePairingCode(pairingCode)
        if (normalized.isBlank()) {
            _pairRequestState.value = PairRequestState.Error("Invalid pairing code format.")
            return
        }
        _pairRequestState.value = PairRequestState.Loading
        viewModelScope.launch {
            val guardianName = prefsRepository.settings.value.displayName
            val result = notificationRepository.sendPairRequest(normalized, guardianName)
            if (result.isSuccess) {
                repository.addPairedWard(normalized, wardName.trim())
                _pairRequestState.value = PairRequestState.Success(
                    "Pairing request sent! Waiting for the ward's approval."
                )
            } else {
                _pairRequestState.value = PairRequestState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send pairing request."
                )
            }
        }
    }

    fun deletePairedWard(id: Long) {
        viewModelScope.launch { repository.deletePairedWard(id) }
    }

    fun resetPairRequestState() {
        _pairRequestState.value = PairRequestState.Idle
    }
}
