package com.womensafety.sos.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.pref.UserPreferencesRepository
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.EmergencyRepository
import com.womensafety.sos.service.EmergencySosService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmergencyViewModel(
    private val emergencyRepository: EmergencyRepository = ServiceLocator.emergencyRepository,
    private val userPreferences: UserPreferencesRepository = ServiceLocator.userPreferences
) : ViewModel() {

    private val _emergencyId = MutableStateFlow("")
    val emergencyId: StateFlow<String> = _emergencyId.asStateFlow()

    val isEmergencyActive: StateFlow<Boolean> = _emergencyId
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startEmergency(context: Context, wardName: String = "") {
        viewModelScope.launch {
            try {
                val resolvedName = wardName.ifBlank { userPreferences.settings.value.displayName }.ifBlank { "Ward" }
                val res = emergencyRepository.startEmergency(resolvedName)
                if (res.isSuccess) {
                    val id = res.getOrThrow()
                    _emergencyId.value = id
                    EmergencySosService.start(context, id)
                } else {
                    Log.e(TAG, "Failed to start emergency", res.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting emergency", e)
            }
        }
    }

    fun stopEmergency(context: Context) {
        viewModelScope.launch {
            try {
                val currentId = _emergencyId.value
                if (currentId.isNotBlank()) {
                    emergencyRepository.resolveEmergency(currentId)
                }
                EmergencySosService.stop(context)
                _emergencyId.value = ""
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping emergency", e)
            }
        }
    }

    fun setEmergencyId(id: String) {
        _emergencyId.value = id
    }

    companion object {
        private const val TAG = "EmergencyViewModel"
    }
}
