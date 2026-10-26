package com.womensafety.sos.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * High-reliability accelerometer shake detector calibrated for emergency SOS triggering.
 *
 * Normalizes acceleration into g-force (1.0g = earth gravity at rest).
 * Filters out casual movement, walking, and table placement by requiring multiple
 * vigorous shake cycles exceeding [SHAKE_THRESHOLD_G] within [SHAKE_COUNT_RESET_TIME_MS].
 */
class ShakeDetector(
    private val requiredShakes: Int = REQUIRED_SHAKES_DEFAULT,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    constructor(onShakeDetected: () -> Unit) : this(REQUIRED_SHAKES_DEFAULT, onShakeDetected)

    private var lastShakeTimestamp: Long = 0L
    private var shakeCount = 0

    companion object {
        // G-force threshold: normal motion is ~1.0g; intentional vigorous shakes reach 2.5g - 4.5g
        const val SHAKE_THRESHOLD_G = 2.7f
        // Minimum time (ms) between distinct shakes to ignore sensor bounce/jitter in the same stroke
        const val SHAKE_SLOP_TIME_MS = 300L
        // Timeout (ms) to complete the required shake sequence before resetting counter
        const val SHAKE_COUNT_RESET_TIME_MS = 2500L
        // Default number of distinct shakes required to trigger SOS
        const val REQUIRED_SHAKES_DEFAULT = 3
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Normalize acceleration against standard Earth gravity (9.80665 m/s^2)
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Net g-force vector magnitude
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        val now = System.currentTimeMillis()

        // Reset shake count if too much time has elapsed since the last shake peak
        if (now - lastShakeTimestamp > SHAKE_COUNT_RESET_TIME_MS) {
            shakeCount = 0
        }

        // Check if current g-force exceeds emergency shake threshold
        if (gForce > SHAKE_THRESHOLD_G) {
            // Debounce: ensure at least SHAKE_SLOP_TIME_MS has elapsed since previous registered shake
            if (now - lastShakeTimestamp >= SHAKE_SLOP_TIME_MS) {
                lastShakeTimestamp = now
                shakeCount++

                if (shakeCount >= requiredShakes) {
                    shakeCount = 0
                    lastShakeTimestamp = 0L
                    onShakeDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun reset() {
        shakeCount = 0
        lastShakeTimestamp = 0L
    }
}
