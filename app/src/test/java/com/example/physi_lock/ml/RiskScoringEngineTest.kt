package com.example.physi_lock.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the hand-written decision rule around the generated RiskModel (level selection +
 * continuous score blending), not the model's own trained accuracy -- that's a training/ML
 * concern (see ml/README.md), not a unit-test concern. Expected values are derived from the
 * real model's own output for each sample, so these tests fail if RiskScoringEngine's mapping
 * logic (documented in its own comments) ever drifts from what it claims to do.
 */
class RiskScoringEngineTest {

    private fun rawProbs(features: RiskFeatures): DoubleArray = RiskModel.score(
        doubleArrayOf(
            features.dailyScreenTimeMin,
            features.avgSessionLengthMin,
            features.appLaunchFrequency,
            features.socialMediaFraction,
            features.bypassAttemptCount,
            features.doomscrollEpisodeCount
        )
    )

    // RiskModel.classes_ is alphabetical: [HIGH, LOW, MODERATE].
    private fun expectedLevel(probs: DoubleArray): RiskLevel {
        val pHigh = probs[0]
        val pLow = probs[1]
        val pModerate = probs[2]
        return when {
            pHigh >= pLow && pHigh >= pModerate -> RiskLevel.HIGH
            pModerate >= pLow -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
    }

    private fun expectedScore(probs: DoubleArray): Double =
        (probs[2] * 0.5 + probs[0] * 1.0).coerceIn(0.0, 1.0)

    private val samples = listOf(
        RiskFeatures(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        RiskFeatures(600.0, 45.0, 30.0, 0.9, 5.0, 3.0),
        RiskFeatures(120.0, 15.0, 8.0, 0.2, 0.0, 0.0),
        RiskFeatures(300.0, 25.0, 15.0, 0.5, 2.0, 1.0),
        RiskFeatures(1000.0, 100.0, 100.0, 1.0, 20.0, 20.0)
    )

    @Test
    fun `level follows the documented pHigh-then-pModerate decision rule`() {
        for (features in samples) {
            val probs = rawProbs(features)
            assertEquals(
                "features=$features probs=${probs.toList()}",
                expectedLevel(probs),
                RiskScoringEngine.score(features).level
            )
        }
    }

    @Test
    fun `continuous score blends pModerate at half weight and pHigh at full weight`() {
        for (features in samples) {
            val probs = rawProbs(features)
            assertEquals(
                expectedScore(probs),
                RiskScoringEngine.score(features).score,
                1e-9
            )
        }
    }

    @Test
    fun `continuous score always stays within 0 and 1`() {
        for (features in samples) {
            val score = RiskScoringEngine.score(features).score
            assertTrue("score=$score out of range for $features", score in 0.0..1.0)
        }
    }
}
