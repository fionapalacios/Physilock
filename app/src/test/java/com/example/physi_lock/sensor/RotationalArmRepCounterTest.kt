package com.example.physi_lock.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationalArmRepCounterTest {

    private val baseConfig = RotationalArmRepConfig(
        repsRequired = 3,
        accelThreshold = 10f,
        gyroThresholdRadS = 3f,
        coOccurrenceWindowMs = 200L
    )

    @Test
    fun `genuine simultaneous accel and gyro spike counts a rep`() {
        val counter = RotationalArmRepCounter(baseConfig)
        assertNull(counter.onSample(MotionSensorType.ACCEL, 15f, 0L))
        val result = counter.onSample(MotionSensorType.GYRO, 5f, 50L)
        assertEquals(1, result)
        assertEquals(1, counter.repCount)
    }

    @Test
    fun `sustained low-magnitude sway or jitter never counts a rep`() {
        val counter = RotationalArmRepCounter(baseConfig)
        var t = 0L
        repeat(50) {
            assertNull(counter.onSample(MotionSensorType.ACCEL, 3f, t))
            assertNull(counter.onSample(MotionSensorType.GYRO, 1f, t + 10))
            t += 100
        }
        assertEquals(0, counter.repCount)
    }

    @Test
    fun `stale value combo outside the co-occurrence window does not count`() {
        val counter = RotationalArmRepCounter(baseConfig)
        assertNull(counter.onSample(MotionSensorType.ACCEL, 15f, 0L))
        assertNull(counter.onSample(MotionSensorType.GYRO, 5f, 500L))
        assertEquals(0, counter.repCount)
    }

    @Test
    fun `co-occurrence window boundary fires at exactly the limit but not one ms beyond`() {
        val firesAtBoundary = RotationalArmRepCounter(baseConfig)
        assertNull(firesAtBoundary.onSample(MotionSensorType.ACCEL, 15f, 0L))
        assertEquals(1, firesAtBoundary.onSample(MotionSensorType.GYRO, 5f, 200L))

        val missesJustPastBoundary = RotationalArmRepCounter(baseConfig)
        assertNull(missesJustPastBoundary.onSample(MotionSensorType.ACCEL, 15f, 0L))
        assertNull(missesJustPastBoundary.onSample(MotionSensorType.GYRO, 5f, 201L))
        assertEquals(0, missesJustPastBoundary.repCount)
    }

    @Test
    fun `hysteresis rearm is required between reps, no double-count from one sustained motion`() {
        val counter = RotationalArmRepCounter(baseConfig)
        assertNull(counter.onSample(MotionSensorType.ACCEL, 15f, 0L))
        assertEquals(1, counter.onSample(MotionSensorType.GYRO, 5f, 50L))

        // Continued high samples without ever dropping below the rearm fraction --
        // should NOT count a second rep.
        assertNull(counter.onSample(MotionSensorType.ACCEL, 15f, 300L))
        assertNull(counter.onSample(MotionSensorType.GYRO, 5f, 350L))
        assertEquals(1, counter.repCount)

        // Drop both magnitudes below rearmFraction (0.45 * threshold) to rearm, then
        // a fresh co-occurring spike should count rep #2.
        assertNull(counter.onSample(MotionSensorType.ACCEL, 1f, 600L))
        assertNull(counter.onSample(MotionSensorType.GYRO, 0.5f, 650L))
        assertNull(counter.onSample(MotionSensorType.ACCEL, 15f, 900L))
        assertEquals(2, counter.onSample(MotionSensorType.GYRO, 5f, 950L))
    }

    @Test
    fun `forSensitivity scales accel and gyro thresholds monotonically LOW less than MODERATE less than HIGH`() {
        val low = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.LOW)
        val moderate = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE)
        val high = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.HIGH)

        assertTrue(low.accelThreshold < moderate.accelThreshold)
        assertTrue(moderate.accelThreshold < high.accelThreshold)
        assertTrue(low.gyroThresholdRadS < moderate.gyroThresholdRadS)
        assertTrue(moderate.gyroThresholdRadS < high.gyroThresholdRadS)
    }

    @Test
    fun `gentle arm variants produce lower thresholds than vigorous variants for the same sensitivity`() {
        val vigorous = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE, armVariant = ChallengeType.ARM_FULL_ROTATION)
        val curl = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE, armVariant = ChallengeType.ARM_BICEP_CURL)
        val gentle = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE, armVariant = ChallengeType.ARM_SWAY)

        assertTrue(gentle.accelThreshold < curl.accelThreshold)
        assertTrue(curl.accelThreshold < vigorous.accelThreshold)
        assertTrue(gentle.gyroThresholdRadS < curl.gyroThresholdRadS)
        assertTrue(curl.gyroThresholdRadS < vigorous.gyroThresholdRadS)

        // A null variant (Deep Work / Overuse Intervention call sites) matches the
        // vigorous multiplier exactly -- their behavior must stay unchanged.
        val unchanged = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE, armVariant = null)
        assertEquals(vigorous.accelThreshold, unchanged.accelThreshold)
        assertEquals(vigorous.gyroThresholdRadS, unchanged.gyroThresholdRadS)
    }

    @Test
    fun `the same real motion sample fires against a gentle variant but not a vigorous one`() {
        val vigorousConfig = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE, armVariant = ChallengeType.ARM_FULL_ROTATION)
        val gentleConfig = RotationalArmRepConfig.forSensitivity(ChallengeSensitivity.MODERATE, armVariant = ChallengeType.ARM_SWAY)

        // Magnitude representative of a deliberate sway: above the gentle threshold,
        // below the vigorous one.
        val swayAccel = (gentleConfig.accelThreshold + vigorousConfig.accelThreshold) / 2f
        val swayGyro = (gentleConfig.gyroThresholdRadS + vigorousConfig.gyroThresholdRadS) / 2f

        val vigorousCounter = RotationalArmRepCounter(vigorousConfig)
        assertNull(vigorousCounter.onSample(MotionSensorType.ACCEL, swayAccel, 0L))
        assertNull(vigorousCounter.onSample(MotionSensorType.GYRO, swayGyro, 50L))
        assertEquals(0, vigorousCounter.repCount)

        val gentleCounter = RotationalArmRepCounter(gentleConfig)
        assertNull(gentleCounter.onSample(MotionSensorType.ACCEL, swayAccel, 0L))
        val result = gentleCounter.onSample(MotionSensorType.GYRO, swayGyro, 50L)
        assertNotNull(result)
        assertEquals(1, gentleCounter.repCount)
    }
}
