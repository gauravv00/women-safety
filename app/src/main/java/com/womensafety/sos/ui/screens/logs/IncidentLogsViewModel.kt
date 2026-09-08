package com.womensafety.sos.ui.screens.logs

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.SafetyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class IncidentLogsViewModel(
    private val repository: SafetyRepository = ServiceLocator.repository
) : ViewModel() {

    val incidentLogs: StateFlow<List<IncidentLog>> = repository.getAllIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentlyPlayingId = MutableStateFlow<Long?>(null)
    val currentlyPlayingId: StateFlow<Long?> = _currentlyPlayingId.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(incident: IncidentLog) {
        val audioPath = incident.audioFilePath ?: incident.audioCloudUrl
        if (audioPath.isNullOrBlank()) return

        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
                setOnCompletionListener {
                    _currentlyPlayingId.value = null
                }
            }
            _currentlyPlayingId.value = incident.id
        } catch (e: Exception) {
            _currentlyPlayingId.value = null
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // ignore
        }
        _currentlyPlayingId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
