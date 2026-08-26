package com.example.physi_lock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

enum class StepChallengeMode { WALK, JOG }

// Shared by the Walk and Jog challenges. Uses Android's built-in step-detector
// sensor (one discrete event per step) rather than custom accelerometer-only
// step-counting math.
class StepChallengeDetector(
    context: Context,
    private val mode: StepChallengeMode,
    private val sensitivity: ChallengeSensitivity = ChallengeSensitivity.MODERATE,
    private val onProgress: (stepsSoFar: Int, elapsedMs: Long, cadenceStepsPerMin: Int) -> Unit,
    private val onComplete: () -> Unit,
    private val onCadenceDropped: (() -> Unit)? = null,
    private val onUnavailable: (() -> Unit)? = null
) : SensorEventListener, ChallengeDetector {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var stepCount = 0
    private var isFinished = false

    // Jog-only continuous-cadence tracking.
    private val recentStepTimestamps = ArrayDeque<Long>()
    private var sustainedJogStartTime: Long? = null
    private val cadenceGraceMs = 3_000L

    // Rolling accelerometer magnitude samples, used only as a "still actually
    // moving" cross-check for Jog's continuous-motion requirement.
    private val recentAccelMagnitudes = ArrayDeque<Float>()
    private val accelWindowMs = 2_000L
    private val recentAccelTimestamps = ArrayDeque<Long>()
    private val minMovingStdDev = 0.5f

    override fun start() {
        if (stepSensor == null) {
            onUnavailable?.invoke()
            return
        }
        stepCount = 0
        isFinished = false
        sustainedJogStartTime = null
        recentStepTimestamps.clear()
        recentAccelMagnitudes.clear()
        recentAccelTimestamps.clear()
        onProgress(0, 0L, 0)
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        if (mode == StepChallengeMode.JOG) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun stop() {
        isFinished = true
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isFinished) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> handleStep()
            Sensor.TYPE_ACCELEROMETER -> handleAccelSample(event.values)
            else -> return
        }
    }

    private fun handleStep() {
        val now = System.currentTimeMillis()
        stepCount++
        recentStepTimestamps.addLast(now)
        while (recentStepTimestamps.size > 10) recentStepTimestamps.removeFirst()

        when (mode) {
            StepChallengeMode.WALK -> {
                onProgress(stepCount, 0L, 0)
                if (stepCount >= sensitivity.walkStepsRequired) {
                    finish()
                }
            }
            StepChallengeMode.JOG -> {
                val stepsPerMinute = currentCadence(now)
                val cadenceOk = stepsPerMinute >= sensitivity.jogMinStepsPerMin
                val movingOk = isContinuouslyMoving(now)

                if (cadenceOk && movingOk) {
                    if (sustainedJogStartTime == null) sustainedJogStartTime = now
                } else {
                    val start = sustainedJogStartTime
                    if (start != null && now - start > cadenceGraceMs) {
                        sustainedJogStartTime = null
                        onCadenceDropped?.invoke()
                    }
                }

                val elapsed = sustainedJogStartTime?.let { now - it } ?: 0L
                onProgress(stepCount, elapsed, stepsPerMinute)

                if (elapsed >= sensitivity.jogDurationMs) {
                    finish()
                }
            }
        }
    }

    private fun currentCadence(now: Long): Int {
        if (recentStepTimestamps.size < 2) return 0
        val windowStart = recentStepTimestamps.first()
        val windowMs = (now - windowStart).coerceAtLeast(1L)
        return ((recentStepTimestamps.size - 1) * 60_000f / windowMs).toInt()
    }

    private fun handleAccelSample(values: FloatArray) {
        val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val now = System.currentTimeMillis()
        recentAccelMagnitudes.addLast(magnitude)
        recentAccelTimestamps.addLast(now)
        while (recentAccelTimestamps.isNotEmpty() && now - recentAccelTimestamps.first() > accelWindowMs) {
            recentAccelTimestamps.removeFirst()
            recentAccelMagnitudes.removeFirst()
        }
    }

    private fun isContinuouslyMoving(now: Long): Boolean {
        if (recentAccelMagnitudes.size < 3) return false
        val mean = recentAccelMagnitudes.average()
        val variance = recentAccelMagnitudes.sumOf { (it - mean) * (it - mean) } / recentAccelMagnitudes.size
        return sqrt(variance) >= minMovingStdDev
    }

    private fun finish() {
        isFinished = true
        stop()
        onComplete()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
