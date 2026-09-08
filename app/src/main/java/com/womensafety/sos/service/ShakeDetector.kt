package com.womensafety.sos.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShakeDetected: () -> Unit) : SensorEventListener {

    private var lastTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeCount = 0

    companion object {
        private const val SHAKE_THRESHOLD = 800 // g-force threshold ratio
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > 100) {
            val timeDiff = currentTime - lastTime
            lastTime = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val speed = sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)).toDouble()) / timeDiff * 10000

            if (speed > SHAKE_THRESHOLD) {
                if (currentTime - lastTime > SHAKE_SLOP_TIME_MS) {
                    shakeCount++
                    if (shakeCount >= 2) {
                        shakeCount = 0
                        onShakeDetected()
                    }
                }
            }

            if (currentTime - lastTime > SHAKE_COUNT_RESET_TIME_MS) {
                shakeCount = 0
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
