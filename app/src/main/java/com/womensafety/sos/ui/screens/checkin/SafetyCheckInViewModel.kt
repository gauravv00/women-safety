package com.womensafety.sos.ui.screens.checkin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SafetyCheckInViewModel : ViewModel() {

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(minutes: Int, context: Context, onTimerExpired: () -> Unit) {
        stopTimer()
        _remainingSeconds.value = minutes * 60
        _isTimerRunning.value = true

        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }
            if (_isTimerRunning.value && _remainingSeconds.value <= 0) {
                _isTimerRunning.value = false
                onTimerExpired()
            }
        }
    }

    fun markAsSafe() {
        stopTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingSeconds.value = 0
    }
}
