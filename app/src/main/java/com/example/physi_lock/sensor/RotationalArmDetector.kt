package com.example.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

// A rep counts when a large linear acceleration AND a large gyroscope rotation rate
// happen together, genuinely co-occurring (not just both independently "recently
// high") -- this one combined gate covers side-to-side swings, up/down raises, and
// 360 deg rotation alike, without needing per-axis/direction classification. The
// actual rep-counting decision (thresholds, rearm hysteresis, co-occurrence timing)
// lives in RotationalArmRepCounter/RotationalArmRepConfig -- this class is a thin
// SensorEventListener wrapper around it. See RotationalArmRepConfig's own doc comment
// for why the co-occurrence gate exists.
class RotationalArmDetector(
    context: Context,
    private val sensitivity: ChallengeSensitivity = ChallengeSensitivity.MODERATE,
    private val onProgress: (Int) -> Unit,
    private val onComplete: () -> Unit,
    // Deep Work Mode's "shake 5x to exit" gate is a fixed safety confirmation, not a
    // difficulty-scaled unlock challenge -- lets it request an exact rep count
    // independent of the user's Motion Lock Sensitivity setting.
    repsOverride: Int? = null,
    // Move-to-Unlock's named arm-exercise challenges (front-to-back swing, full
    // rotation, curl, side raise, sway, stretch) each tune amplitude/timing
    // differently -- see RotationalArmRepConfig.forSensitivity. Left null (unchanged
    // behavior) by every other caller (Deep Work's exit gate, Overuse Intervention's
    // Shake/Rotate steps), which don't go through ChallengeType at all.
    private val armVariant: ChallengeType? = null
) : SensorEventListener, ChallengeDetector {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val repsRequired = repsOverride ?: sensitivity.armRepsRequired
    private lateinit var counter: RotationalArmRepCounter

    private var isFinished = false

    override fun start() {
        isFinished = false
        counter = RotationalArmRepCounter(
            RotationalArmRepConfig.forSensitivity(sensitivity, repsRequired, armVariant)
        )
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

        val type: MotionSensorType
        val magnitude: Float
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val (x, y, z) = event.values
                val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                type = MotionSensorType.ACCEL
                magnitude = kotlin.math.abs(gForce)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val (x, y, z) = event.values
                type = MotionSensorType.GYRO
                magnitude = sqrt(x * x + y * y + z * z)
            }
            else -> return
        }

        val newCount = counter.onSample(type, magnitude, System.currentTimeMillis()) ?: return
        onProgress(newCount)

        if (newCount >= repsRequired) {
            isFinished = true
            stop()
            onComplete()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
