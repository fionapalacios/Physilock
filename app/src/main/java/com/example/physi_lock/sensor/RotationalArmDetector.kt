package com.example.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

// A rep counts when a large linear acceleration AND a large gyroscope rotation rate
// happen together — this one combined gate covers side-to-side swings, up/down raises,
// and 360 deg rotation alike (all produce a big limb displacement plus rotation), without
// needing per-axis/direction classification.
//
// 2026-09-04: added a real hysteresis (arm/rearm) gate on top of the threshold check
// above -- the previous version could double-count a single sustained motion (e.g. one
// slow continuous swing, or hand jitter that happens to stay above threshold) as multiple
// reps, since a debounce timer alone doesn't require the signal to ever *drop* between
// counts. A genuine shake/swing is oscillatory: peak, then a real return toward rest,
// then the next peak. armed only re-arms once BOTH accel and gyro magnitude drop back
// below a rearm fraction of their thresholds, so counting a rep now requires an actual
// back-and-forth motion, not just staying loud for a while. Thresholds also raised ~20%
// (rearmFraction below the peak) to require a more deliberate motion.
class RotationalArmDetector(
    context: Context,
    private val sensitivity: ChallengeSensitivity = ChallengeSensitivity.MODERATE,
    private val onProgress: (Int) -> Unit,
    private val onComplete: () -> Unit,
    // Deep Work Mode's "shake 5x to exit" gate is a fixed safety confirmation, not a
    // difficulty-scaled unlock challenge -- lets it request an exact rep count
    // independent of the user's Motion Lock Sensitivity setting.
    repsOverride: Int? = null
) : SensorEventListener, ChallengeDetector {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val repsRequired = repsOverride ?: sensitivity.armRepsRequired
    private val accelThreshold = sensitivity.armGForceThreshold * 0.6f
    private val gyroThresholdRadS = 3.0f
    private val rearmFraction = 0.45f
    private val repDebounceMs = 250L

    private var repCount = 0
    private var lastRepTime = 0L
    private var isFinished = false
    private var armed = true

    private var lastAccelMagnitude = 0f
    private var lastGyroMagnitude = 0f

    override fun start() {
        repCount = 0
        isFinished = false
        armed = true
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

        // Rearm only once the motion has genuinely settled back down on both axes --
        // this is what forces an oscillation (out-and-back) instead of one sustained push.
        if (!armed && lastAccelMagnitude < accelThreshold * rearmFraction && lastGyroMagnitude < gyroThresholdRadS * rearmFraction) {
            armed = true
        }

        if (armed && lastAccelMagnitude > accelThreshold && lastGyroMagnitude > gyroThresholdRadS) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRepTime < repDebounceMs) return

            armed = false
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
