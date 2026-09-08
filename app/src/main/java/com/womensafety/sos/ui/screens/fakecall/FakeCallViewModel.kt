package com.womensafety.sos.ui.screens.fakecall

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCallViewModel : ViewModel() {
    private val _callerName = MutableStateFlow("Mom (Incoming)")
    val callerName: StateFlow<String> = _callerName.asStateFlow()

    private val _isCallAnswered = MutableStateFlow(false)
    val isCallAnswered: StateFlow<Boolean> = _isCallAnswered.asStateFlow()

    fun setCallerName(name: String) {
        _callerName.value = name
    }

    fun answerCall() {
        _isCallAnswered.value = true
    }
}
