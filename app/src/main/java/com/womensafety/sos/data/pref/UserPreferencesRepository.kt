package com.womensafety.sos.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SafetySettings(
    val shakeToTriggerEnabled: Boolean = true,
    val loudSirenEnabled: Boolean = true,
    val autoCallTopContact: Boolean = true,
    val autoCallDelaySeconds: Int = 10,
    val sequentialEscalationEnabled: Boolean = true,
    val batteryAwarePolling: Boolean = true,
    val guardianMonitoringEnabled: Boolean = true,
    val displayName: String = "User",
    val myPairingCode: String = ""
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("women_safety_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SafetySettings> = _settings.asStateFlow()

    private fun loadSettings(): SafetySettings {
        var code = prefs.getString("my_pairing_code", null)
        if (code.isNullOrBlank()) {
            code = generatePairingCode()
            prefs.edit().putString("my_pairing_code", code).apply()
        }
        return SafetySettings(
            shakeToTriggerEnabled = prefs.getBoolean("shake_to_trigger", true),
            loudSirenEnabled = prefs.getBoolean("loud_siren", true),
            autoCallTopContact = prefs.getBoolean("auto_call_top", true),
            autoCallDelaySeconds = prefs.getInt("auto_call_delay", 10),
            sequentialEscalationEnabled = prefs.getBoolean("sequential_escalation", true),
            batteryAwarePolling = prefs.getBoolean("battery_aware", true),
            guardianMonitoringEnabled = prefs.getBoolean("guardian_monitoring_enabled", true),
            displayName = prefs.getString("user_display_name", "My Phone") ?: "My Phone",
            myPairingCode = code
        )
    }

    private fun generatePairingCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val randomStr = (1..6)
            .map { allowedChars.random() }
            .joinToString("")
        return "SOS-$randomStr"
    }

    fun getPairingCode(): String {
        return _settings.value.myPairingCode
    }

    fun updateDisplayName(name: String) {
        val clean = name.trim()
        prefs.edit().putString("user_display_name", clean).apply()
        _settings.value = _settings.value.copy(displayName = clean)
    }

    fun updateGuardianMonitoring(enabled: Boolean) {
        prefs.edit().putBoolean("guardian_monitoring_enabled", enabled).apply()
        _settings.value = _settings.value.copy(guardianMonitoringEnabled = enabled)
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
