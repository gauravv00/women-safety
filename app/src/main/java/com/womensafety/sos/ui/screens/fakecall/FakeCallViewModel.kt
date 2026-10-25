package com.womensafety.sos.ui.screens.fakecall

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class FakeCallStage {
    SETUP,
    INCOMING,
    IN_CALL,
    ENDED
}

class FakeCallViewModel : ViewModel(), TextToSpeech.OnInitListener {

    private val TAG = "FakeCallViewModel"

    private val _stage = MutableStateFlow(FakeCallStage.INCOMING)
    val stage: StateFlow<FakeCallStage> = _stage.asStateFlow()

    private val _callerName = MutableStateFlow("Mom")
    val callerName: StateFlow<String> = _callerName.asStateFlow()

    private val _callerNumber = MutableStateFlow("+1 (555) 019-2834")
    val callerNumber: StateFlow<String> = _callerNumber.asStateFlow()

    private val _callSeconds = MutableStateFlow(0)
    val callSeconds: StateFlow<Int> = _callSeconds.asStateFlow()

    private val _scheduledCountdown = MutableStateFlow(0)
    val scheduledCountdown: StateFlow<Int> = _scheduledCountdown.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isOnHold = MutableStateFlow(false)
    val isOnHold: StateFlow<Boolean> = _isOnHold.asStateFlow()

    private val _showKeypad = MutableStateFlow(false)
    val showKeypad: StateFlow<Boolean> = _showKeypad.asStateFlow()

    private val _dialedDigits = MutableStateFlow("")
    val dialedDigits: StateFlow<String> = _dialedDigits.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var vibrateJob: Job? = null
    private var ringtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var activeVibrator: Vibrator? = null
    private var appContext: Context? = null

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isTtsReady = true
        }
    }

    fun setCallerDetails(name: String, number: String) {
        _callerName.value = name.ifBlank { "Mom" }
        _callerNumber.value = number.ifBlank { "Mobile" }
    }

    fun startImmediateIncoming(context: Context) {
        _stage.value = FakeCallStage.INCOMING
        startRinging(context)
    }

    fun scheduleCall(seconds: Int, context: Context) {
        _stage.value = FakeCallStage.SETUP
        _scheduledCountdown.value = seconds
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_scheduledCountdown.value > 0) {
                delay(1000L)
                _scheduledCountdown.value -= 1
            }
            startImmediateIncoming(context)
        }
    }

    fun cancelSchedule() {
        countdownJob?.cancel()
        _scheduledCountdown.value = 0
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun startRinging(context: Context) {
        stopRinging()
        appContext = context.applicationContext
        try {
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context.applicationContext, ringUri)
            ringtone?.play()

            activeVibrator = getVibrator(context.applicationContext)

            // Pulse finite 1-second vibration in a coroutine loop.
            // Using a finite one-shot pulse guarantees that if the app is killed or
            // the user picks up/declines, the vibration physically terminates and
            // never repeats indefinitely on the Android system service.
            vibrateJob?.cancel()
            vibrateJob = viewModelScope.launch {
                while (_stage.value == FakeCallStage.INCOMING) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            activeVibrator?.vibrate(
                                VibrationEffect.createOneShot(1000L, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            activeVibrator?.vibrate(1000L)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Vibrate pulse error: ${e.message}")
                    }
                    delay(2000L) // 1s vibrate + 1s pause
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ringtone/vibrate error: ${e.message}")
        }
    }

    fun stopRinging() {
        try {
            ringtone?.stop()
            ringtone = null
        } catch (e: Exception) {
            // ignore
        }

        // Cancel pulse coroutine
        vibrateJob?.cancel()
        vibrateJob = null

        // Explicitly cancel active vibrator
        try {
            activeVibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling active vibrator: ${e.message}")
        }

        // Secondary fallback: cancel directly on system service via context
        appContext?.let { ctx ->
            try {
                getVibrator(ctx)?.cancel()
            } catch (e: Exception) {
                // ignore
            }
        }
        activeVibrator = null
    }

    fun answerCall(context: Context) {
        stopRinging()
        _stage.value = FakeCallStage.IN_CALL
        _callSeconds.value = 0

        // Start timer counting up
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_stage.value == FakeCallStage.IN_CALL) {
                delay(1000L)
                _callSeconds.value += 1

                // Simulate realistic speech voice after 2 seconds
                if (_callSeconds.value == 2 && isTtsReady && !_isOnHold.value) {
                    playRealisticVoice()
                }
            }
        }
    }

    private fun playRealisticVoice() {
        try {
            val phrase = "Hey, are you there? I'm waiting for you right outside. Come quickly, okay? I'm watching the street for you."
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "fake_call_voice")
        } catch (e: Exception) {
            Log.w(TAG, "TTS speak error: ${e.message}")
        }
    }

    fun declineCall(context: Context, onDismiss: () -> Unit) {
        stopRinging()
        _stage.value = FakeCallStage.ENDED
        onDismiss()
    }

    fun endCall(onDismiss: () -> Unit) {
        stopRinging()
        timerJob?.cancel()
        tts?.stop()
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (e: Exception) {
            // ignore
        }
        _stage.value = FakeCallStage.ENDED
        onDismiss()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleHold() {
        _isOnHold.value = !_isOnHold.value
        if (_isOnHold.value) {
            tts?.stop()
        }
    }

    fun toggleKeypad() {
        _showKeypad.value = !_showKeypad.value
    }

    fun pressKeypadDigit(digit: Char) {
        _dialedDigits.value += digit
        try {
            val tone = when (digit) {
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '0' -> ToneGenerator.TONE_DTMF_0
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            toneGenerator?.startTone(tone, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Keypad tone error: ${e.message}")
        }
    }

    fun deleteKeypadDigit() {
        if (_dialedDigits.value.isNotEmpty()) {
            _dialedDigits.value = _dialedDigits.value.dropLast(1)
        }
    }

    fun showSetup() {
        _stage.value = FakeCallStage.SETUP
    }

    override fun onCleared() {
        super.onCleared()
        stopRinging()
        timerJob?.cancel()
        countdownJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        toneGenerator?.release()
    }
}
