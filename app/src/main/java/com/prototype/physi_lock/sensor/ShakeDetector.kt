package com.prototype.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val shakesRequired: Int = 10,
    private val onShakeProgress: (Int) -> Unit,
    private val onShakeComplete: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastShakeTime = 0L
    private var isFinished = false

    private val shakeDebounceMs = 220L

    // You must shake it aggressively with your hand for it to count. Bumping the table won't work.
    private val shakeThreshold = 23.5f

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
