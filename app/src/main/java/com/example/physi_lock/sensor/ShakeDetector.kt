package com.example.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

// Mirrors UserConfiguration.motionLockSensitivity ("LOW"/"MEDIUM"/"HIGH") — the
// single source of truth for how the setting maps to detector behavior, shared
// between the actual challenge (ShakeDetector) and its display in Settings.
enum class ShakeSensitivity(val gForceThreshold: Float, val shakesRequired: Int) {
    LOW(18f, 6),
    MEDIUM(23.5f, 10),
    HIGH(28f, 15);

    companion object {
        fun fromLabel(label: String?): ShakeSensitivity = when (label) {
            "LOW" -> LOW
            "HIGH" -> HIGH
            else -> MEDIUM
        }
    }
}

class ShakeDetector(
    context: Context,
    private val sensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    private val onShakeProgress: (Int) -> Unit,
    private val onShakeComplete: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val shakesRequired = sensitivity.shakesRequired
    private var shakeCount = 0
    private var lastShakeTime = 0L
    private var isFinished = false

    private val shakeDebounceMs = 220L

    // You must shake it aggressively with your hand for it to count; bumping the table won't work.
    private val shakeThreshold = sensitivity.gForceThreshold

    fun start() {
        shakeCount = 0
        isFinished = false
        onShakeProgress(0)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        isFinished = true
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isFinished) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (gForce > shakeThreshold) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastShakeTime < shakeDebounceMs) return

            lastShakeTime = currentTime
            shakeCount++

            onShakeProgress(shakeCount)

            if (shakeCount >= shakesRequired) {
                isFinished = true
                stop()
                shakeCount = 0
                onShakeComplete()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}