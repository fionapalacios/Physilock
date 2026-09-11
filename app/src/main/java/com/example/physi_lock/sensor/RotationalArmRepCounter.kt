package com.example.physi_lock.sensor

enum class MotionSensorType { ACCEL, GYRO }

// 2026-09-09: replaces the old "compare two independently-latched last-known values"
// gate. That approach compared accel/gyro magnitudes updated by unsynchronized sensor
// event streams -- at the slow SENSOR_DELAY_UI sampling rate, a stale high reading from
// one sensor could sit latched for hundreds of ms and combine with an unrelated later
// spike from the other sensor to falsely fire a rep, even though the two never actually
// peaked together (this is what let incidental sway/jitter register as real reps). This
// config instead drives a timestamp co-occurrence gate -- see RotationalArmRepCounter.
data class RotationalArmRepConfig(
    val repsRequired: Int,
    val accelThreshold: Float,
    val gyroThresholdRadS: Float,
    val rearmFraction: Float = 0.45f,
    val repDebounceMs: Long = 250L,
    val coOccurrenceWindowMs: Long = 200L
) {
    companion object {
        private const val ACCEL_THRESHOLD_SCALE = 0.6f

        fun forSensitivity(
            sensitivity: ChallengeSensitivity,
            repsOverride: Int? = null,
            // null preserves exact pre-existing behavior for callers that construct
            // RotationalArmDetector directly without an arm-exercise variant (Deep
            // Work's shake-exit gate, Overuse Intervention's Shake/Rotate steps).
            armVariant: ChallengeType? = null
        ): RotationalArmRepConfig {
            val amplitude = amplitudeMultiplierFor(armVariant)
            return RotationalArmRepConfig(
                repsRequired = repsOverride ?: sensitivity.armRepsRequired,
                accelThreshold = sensitivity.armGForceThreshold * ACCEL_THRESHOLD_SCALE * amplitude,
                gyroThresholdRadS = sensitivity.armGyroThresholdRadS * amplitude,
                repDebounceMs = repDebounceMsFor(armVariant),
                coOccurrenceWindowMs = coOccurrenceWindowMsFor(armVariant)
            )
        }

        // Vigorous variants keep today's exact thresholds (multiplier 1.0). Curl is a
        // smaller-radius motion than a full swing/rotation. Sway/Stretch are
        // intentionally lower-amplitude challenges than the vigorous ones, gated by the
        // real co-occurrence/rearm checks below so passive jitter can't complete them --
        // but 2026-09-09 on-device testing found 0.55 too low: picking the phone up off
        // a table and setting it down produces a single co-occurring accel+gyro spike
        // that's physically similar to a deliberate light sway, and completed the
        // challenge. Raised to 0.75 (just below curl) so ordinary handling stays below
        // threshold while a real deliberate sway/stretch still clears it -- there's no
        // amplitude value that perfectly separates "picked up the phone" from "a very
        // light sway" (see the RotationalArmDetector split's "Future consideration" doc
        // in ChallengeType.kt -- that needs real orientation-aware shape detection, out
        // of scope here), so this is a calibration best-effort, not a structural fix.
        private fun amplitudeMultiplierFor(variant: ChallengeType?): Float = when (variant) {
            ChallengeType.ARM_SWING_FRONT_BACK, ChallengeType.ARM_FULL_ROTATION, ChallengeType.ARM_SIDE_RAISE -> 1.0f
            ChallengeType.ARM_BICEP_CURL -> 0.85f
            ChallengeType.ARM_SWAY, ChallengeType.ARM_STRETCH -> 0.75f
            else -> 1.0f
        }

        // Gentle variants have a naturally slower cadence -- widen the debounce and
        // co-occurrence window slightly so a real slow sway/stretch rep isn't missed.
        private fun repDebounceMsFor(variant: ChallengeType?): Long = when (variant) {
            ChallengeType.ARM_SWAY, ChallengeType.ARM_STRETCH -> 400L
            else -> 250L
        }

        private fun coOccurrenceWindowMsFor(variant: ChallengeType?): Long = when (variant) {
            ChallengeType.ARM_SWAY, ChallengeType.ARM_STRETCH -> 250L
            else -> 200L
        }
    }
}

/**
 * Pure, Android-free rep-counting state machine for [RotationalArmDetector]. Extracted
 * so the decision logic is directly unit-testable (Android's SensorEvent has no public
 * constructor, so this can't be exercised via a real detector in a JVM unit test).
 */
class RotationalArmRepCounter(private val config: RotationalArmRepConfig) {
    var repCount = 0
        private set

    private var armed = true
    private var lastRepTimeMs = Long.MIN_VALUE / 2

    private var lastAccelMagnitude = 0f
    private var lastGyroMagnitude = 0f
    private var lastAccelPeakTimeMs: Long? = null
    private var lastGyroPeakTimeMs: Long? = null

    /** Feed one sample. Returns the new rep count if this sample just completed a rep. */
    fun onSample(type: MotionSensorType, magnitude: Float, timestampMs: Long): Int? {
        when (type) {
            MotionSensorType.ACCEL -> lastAccelMagnitude = magnitude
            MotionSensorType.GYRO -> lastGyroMagnitude = magnitude
        }

        // Rearm only once the motion has genuinely settled back down on both axes --
        // this forces an oscillation (out-and-back) instead of one sustained push.
        if (!armed &&
            lastAccelMagnitude < config.accelThreshold * config.rearmFraction &&
            lastGyroMagnitude < config.gyroThresholdRadS * config.rearmFraction
        ) {
            armed = true
        }

        val threshold = if (type == MotionSensorType.ACCEL) config.accelThreshold else config.gyroThresholdRadS
        if (magnitude <= threshold) return null

        when (type) {
            MotionSensorType.ACCEL -> lastAccelPeakTimeMs = timestampMs
            MotionSensorType.GYRO -> lastGyroPeakTimeMs = timestampMs
        }
        if (!armed) return null

        // Co-occurrence gate: the *other* sensor must have crossed its own threshold
        // recently too -- not just be sitting at a stale high value from a much
        // earlier, unrelated moment.
        val otherPeakTimeMs = if (type == MotionSensorType.ACCEL) lastGyroPeakTimeMs else lastAccelPeakTimeMs
        if (otherPeakTimeMs == null) return null
        if (kotlin.math.abs(timestampMs - otherPeakTimeMs) > config.coOccurrenceWindowMs) return null
        if (timestampMs - lastRepTimeMs < config.repDebounceMs) return null

        armed = false
        lastRepTimeMs = timestampMs
        repCount++
        return repCount
    }
}
