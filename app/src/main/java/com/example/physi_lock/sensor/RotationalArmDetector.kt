package com.example.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

// Replaces the old single-axis shake check. A rep counts when a large linear
// acceleration AND a large gyroscope rotation rate happen together — this one
// combined gate covers side-to-side swings, up/down raises, and 360 deg
// rotation alike (all produce a big limb displacement plus rotation), without
// needing per-axis/direction classification.
class RotationalArmDetector(
    context: Context,
    private val sensitivity: ChallengeSensitivity = ChallengeSensitivity.MODERATE,
    private val onProgress: (Int) -> Unit,
    private val onComplete: () -> Unit
) : SensorEventListener, ChallengeDetector {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val repsRequired = sensitivity.armRepsRequired
    private val accelThreshold = sensitivity.armGForceThreshold * 0.5f
    private val gyroThresholdRadS = 2.5f
    private val repDebounceMs = 300L

    private var repCount = 0
    private var lastRepTime = 0L
    private var isFinished = false

    private var lastAccelMagnitude = 0f
    private var lastGyroMagnitude = 0f

    override fun start() {
        repCount = 0
        isFinished = false
        onProgress(0)
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun stop() {
        isFinished = true
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isFinished) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val (x, y, z) = event.values
                val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                lastAccelMagnitude = kotlin.math.abs(gForce)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val (x, y, z) = event.values
                lastGyroMagnitude = sqrt(x * x + y * y + z * z)
            }
            else -> return
        }

        if (lastAccelMagnitude > accelThreshold && lastGyroMagnitude > gyroThresholdRadS) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime < repDebounceMs) return

            lastRepTime = currentTime
            repCount++
            onProgress(repCount)

            if (repCount >= repsRequired) {
                isFinished = true
                stop()
                repCount = 0
                onComplete()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
