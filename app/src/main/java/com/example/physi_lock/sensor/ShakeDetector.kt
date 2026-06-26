package com.example.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val shakesRequired: Int = 5, /// mao ni i customize, the higher the users addiction risk score = require more shakes
    private val shakeThreshold: Float = 30.5f, /// lower = more sensitive (easier to trigger accidentally), higher = harder shake;
    private val shakeWindowMs: Long = 1500L,
    private val onShakeComplete: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var firstShakeTime = 0L
    private var lastShakeTime = 0L
    private val debounceMs = 100L // prevents one physical shake being counted multiple times

    fun start() {
        shakeCount = 0
        firstShakeTime = 0L
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // total acceleration minus gravity, so holding the phone still reads ~0
        val magnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (magnitude > shakeThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime < debounceMs) return
            lastShakeTime = now

            if (shakeCount == 0 || now - firstShakeTime > shakeWindowMs) {
                shakeCount = 1
                firstShakeTime = now
            } else {
                shakeCount++
            }

            if (shakeCount >= shakesRequired) {
                shakeCount = 0
                onShakeComplete()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}