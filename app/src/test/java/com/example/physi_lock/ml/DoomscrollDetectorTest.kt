package com.example.physi_lock.ml

import kotlin.math.exp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests DoomscrollDetector's threshold-selection logic against the real generated
 * DoomscrollModel's output, not the model's own trained accuracy. Also verifies the
 * documented "recipe" design (project memory "module2-doomscroll-design"): a high-risk user
 * is flagged by a weaker signal than a low-risk user needs, i.e. thresholds only get
 * stricter as risk decreases (LOW=0.75, MODERATE=0.60, HIGH=0.45).
 */
class DoomscrollDetectorTest {

    private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

    private val thresholds = mapOf(
        RiskLevel.LOW to 0.75,
        RiskLevel.MODERATE to 0.60,
        RiskLevel.HIGH to 0.45
    )

    private val samples = listOf(
        DoomscrollInputs(scrollSpeedPerMin = 0.0, maxPauseGapSec = 30.0, hourOfDay = 12.0),
        DoomscrollInputs(scrollSpeedPerMin = 80.0, maxPauseGapSec = 2.0, hourOfDay = 23.0),
        DoomscrollInputs(scrollSpeedPerMin = 40.0, maxPauseGapSec = 10.0, hourOfDay = 8.0),
        DoomscrollInputs(scrollSpeedPerMin = 120.0, maxPauseGapSec = 1.0, hourOfDay = 1.0)
    )

    @Test
    fun `detect matches sigmoid of the model logit against each risk level's threshold`() {
        for (inputs in samples) {
            val logit = DoomscrollModel.score(
                doubleArrayOf(inputs.scrollSpeedPerMin, inputs.maxPauseGapSec, inputs.hourOfDay)
            )
            val probability = sigmoid(logit)
            for ((riskLevel, threshold) in thresholds) {
                assertEquals(
                    "inputs=$inputs riskLevel=$riskLevel probability=$probability",
                    probability >= threshold,
                    DoomscrollDetector.detect(inputs, riskLevel)
                )
            }
        }
    }

    @Test
    fun `a flag at a lower risk level always also flags at every higher risk level`() {
        for (inputs in samples) {
            val flaggedLow = DoomscrollDetector.detect(inputs, RiskLevel.LOW)
            val flaggedModerate = DoomscrollDetector.detect(inputs, RiskLevel.MODERATE)
            val flaggedHigh = DoomscrollDetector.detect(inputs, RiskLevel.HIGH)

            if (flaggedLow) {
                assertEquals("inputs=$inputs: LOW flagged but MODERATE didn't", true, flaggedModerate)
            }
            if (flaggedModerate) {
                assertEquals("inputs=$inputs: MODERATE flagged but HIGH didn't", true, flaggedHigh)
            }
        }
    }
}
