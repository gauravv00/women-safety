package com.womensafety.sos.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderService(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: String? = null
    private val TAG = "AudioRecorderService"

    fun startRecording(): String? {
        try {
            stopRecording() // ensure previous released

            val timeStamp = SimpleDateFormat("yyyyMMDD_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SOS_AUDIO_$timeStamp.mp3"
            val outputFile = File(context.filesDir, fileName)
            currentOutputFile = outputFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Log.d(TAG, "Audio recording started: ${outputFile.absolutePath}")
            return currentOutputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording: ${e.localizedMessage}")
            currentOutputFile = null
            return null
        }
    }

    fun stopRecording(): String? {
        val path = currentOutputFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recorder: ${e.localizedMessage}")
        } finally {
            mediaRecorder = null
        }
        currentOutputFile = null
        return path
    }
}
