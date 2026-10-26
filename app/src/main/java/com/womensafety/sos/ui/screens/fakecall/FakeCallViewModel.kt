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

data class IndianCallerPreset(
    val name: String,
    val number: String,
    val tag: String,
    val voicePhrase: String
)

class FakeCallViewModel : ViewModel(), TextToSpeech.OnInitListener {

    private val TAG = "FakeCallViewModel"

    companion object {
        val INDIAN_PRESETS = listOf(
            IndianCallerPreset(
                name = "Maa",
                number = "+91 98201 54321",
                tag = "Mobile • Jio 4G, India",
                voicePhrase = "Haan beta, kahan tak pahunche? Main gate par hi wait kar rahi hoon, jaldi aao."
            ),
            IndianCallerPreset(
                name = "Papa",
                number = "+91 98112 34567",
                tag = "Mobile • Airtel 5G, Delhi NCR",
                voicePhrase = "Beta sab theek hai na? Main gaadi nikaal chuka hoon, bas do minute mein pahunch raha hoon."
            ),
            IndianCallerPreset(
                name = "Bhaiya",
                number = "+91 99100 87654",
                tag = "Mobile • Vi, Mumbai",
                voicePhrase = "Hey, main live location dekh raha hoon. Stay where you are, I'm right around the corner."
            ),
            IndianCallerPreset(
                name = "Cab Driver (Ola/Uber)",
                number = "+91 98765 43210",
                tag = "White Dzire (DL 1Z A 4421)",
                voicePhrase = "Madam, main location par aa gaya hoon, hazard lights on hain gaadi ki."
            ),
            IndianCallerPreset(
                name = "Police Helpline",
                number = "112",
                tag = "National Emergency Response System",
                voicePhrase = "Police emergency control room. Stay on the line, unit is monitoring your area."
            ),
            IndianCallerPreset(
                name = "Office Manager",
                number = "+91 98200 98765",
                tag = "Work • Gurugram",
                voicePhrase = "Hi, just checking in. Did your cab arrive? Let us know when you reach safely."
            )
        )
    }

    private val _stage = MutableStateFlow(FakeCallStage.INCOMING)
    val stage: StateFlow<FakeCallStage> = _stage.asStateFlow()

    private val _callerName = MutableStateFlow("Maa")
    val callerName: StateFlow<String> = _callerName.asStateFlow()

    private val _callerNumber = MutableStateFlow("+91 98201 54321")
    val callerNumber: StateFlow<String> = _callerNumber.asStateFlow()

    private val _callerTag = MutableStateFlow("Mobile • Jio 4G, India")
    val callerTag: StateFlow<String> = _callerTag.asStateFlow()

    private val _voicePhrase = MutableStateFlow("Haan beta, kahan tak pahunche? Main gate par hi wait kar rahi hoon, jaldi aao.")
    val voicePhrase: StateFlow<String> = _voicePhrase.asStateFlow()

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
            try {
                // Try Indian English locale first for natural authenticity
                val inLocale = Locale("en", "IN")
                val langAvailable = tts?.isLanguageAvailable(inLocale) ?: -1
                if (langAvailable >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = inLocale
                } else {
                    tts?.language = Locale.getDefault()
                }
            } catch (_: Exception) {
                tts?.language = Locale.getDefault()
            }
            isTtsReady = true
        }
    }

    fun setCallerDetails(
        name: String,
        number: String,
        tag: String = "Mobile • Jio 4G, India",
        phrase: String = ""
    ) {
        _callerName.value = name.ifBlank { "Maa" }
        _callerNumber.value = number.ifBlank { "+91 98201 54321" }
        _callerTag.value = tag.ifBlank { "Mobile • India" }
        if (phrase.isNotBlank()) {
            _voicePhrase.value = phrase
        }
    }

    fun applyPreset(preset: IndianCallerPreset) {
        setCallerDetails(preset.name, preset.number, preset.tag, preset.voicePhrase)
    }

    fun startImmediateIncoming(context: Context) {
        _stage.value = FakeCallStage.INCOMING
        startRinging(context)
    }

    fun openSetup() {
        stopRinging()
        _stage.value = FakeCallStage.SETUP
    }

    fun showSetup() = openSetup()

    fun scheduleCall(delaySeconds: Int, context: Context) {
        scheduleCall(context, delaySeconds)
    }

    fun scheduleCall(context: Context, delaySeconds: Int) {
        _stage.value = FakeCallStage.SETUP
        _scheduledCountdown.value = delaySeconds

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = delaySeconds
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _scheduledCountdown.value = remaining
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
        } catch (_: Exception) {}

        vibrateJob?.cancel()
        vibrateJob = null

        try {
            activeVibrator?.cancel()
            activeVibrator = null
        } catch (_: Exception) {}

        appContext?.let { ctx ->
            try {
                val v = getVibrator(ctx)
                v?.cancel()
            } catch (_: Exception) {}
        }
    }

    fun answerCall(context: Context? = null) {
        stopRinging()
        _stage.value = FakeCallStage.IN_CALL
        _callSeconds.value = 0

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_stage.value == FakeCallStage.IN_CALL) {
                delay(1000L)
                _callSeconds.value += 1

                // Simulate authentic spoken dialogue after 1.5 seconds
                if (_callSeconds.value == 2 && isTtsReady && !_isOnHold.value) {
                    playRealisticVoice()
                }
            }
        }
    }

    private fun playRealisticVoice() {
        try {
            val phrase = _voicePhrase.value.ifBlank {
                "Haan beta, kahan tak pahunche? Main gate par hi wait kar rahi hoon, jaldi aao."
            }
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
        } catch (_: Exception) {}
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

    fun pressKeypadDigit(digit: Char) = appendKeypadDigit(digit)

    fun appendKeypadDigit(digit: Char) {
        _dialedDigits.value += digit
        try {
            val tone = when (digit) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            toneGenerator?.startTone(tone, 120)
        } catch (_: Exception) {}
    }

    fun deleteKeypadDigit() {
        if (_dialedDigits.value.isNotEmpty()) {
            _dialedDigits.value = _dialedDigits.value.dropLast(1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRinging()
        timerJob?.cancel()
        countdownJob?.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
    }
}
