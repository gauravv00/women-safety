package com.womensafety.sos.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SafetySettings(
    val shakeToTriggerEnabled: Boolean = true,
    val loudSirenEnabled: Boolean = true,
    val autoCallTopContact: Boolean = true,
    val autoCallDelaySeconds: Int = 10,
    val sequentialEscalationEnabled: Boolean = true,
    val batteryAwarePolling: Boolean = true
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("women_safety_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SafetySettings> = _settings.asStateFlow()

    private fun loadSettings(): SafetySettings {
        return SafetySettings(
            shakeToTriggerEnabled = prefs.getBoolean("shake_to_trigger", true),
            loudSirenEnabled = prefs.getBoolean("loud_siren", true),
            autoCallTopContact = prefs.getBoolean("auto_call_top", true),
            autoCallDelaySeconds = prefs.getInt("auto_call_delay", 10),
            sequentialEscalationEnabled = prefs.getBoolean("sequential_escalation", true),
            batteryAwarePolling = prefs.getBoolean("battery_aware", true)
        )
    }

    fun updateShakeToTrigger(enabled: Boolean) {
        prefs.edit().putBoolean("shake_to_trigger", enabled).apply()
        _settings.value = _settings.value.copy(shakeToTriggerEnabled = enabled)
    }

    fun updateLoudSiren(enabled: Boolean) {
        prefs.edit().putBoolean("loud_siren", enabled).apply()
        _settings.value = _settings.value.copy(loudSirenEnabled = enabled)
    }

    fun updateAutoCallTopContact(enabled: Boolean) {
        prefs.edit().putBoolean("auto_call_top", enabled).apply()
        _settings.value = _settings.value.copy(autoCallTopContact = enabled)
    }

    fun updateAutoCallDelay(seconds: Int) {
        prefs.edit().putInt("auto_call_delay", seconds).apply()
        _settings.value = _settings.value.copy(autoCallDelaySeconds = seconds)
    }

    fun updateSequentialEscalation(enabled: Boolean) {
        prefs.edit().putBoolean("sequential_escalation", enabled).apply()
        _settings.value = _settings.value.copy(sequentialEscalationEnabled = enabled)
    }

    fun updateBatteryAware(enabled: Boolean) {
        prefs.edit().putBoolean("battery_aware", enabled).apply()
        _settings.value = _settings.value.copy(batteryAwarePolling = enabled)
    }
}
