package com.example.physi_lock.ml

enum class RiskLevel(val label: String) {
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High")
}

data class RiskAssessment(val level: RiskLevel, val score: Double)

/**
 * Module 2 (AI-Based Behavior Analysis): Random Forest risk classifier, trained offline
 * in ml/train_risk_model.py and transpiled to plain Java via m2cgen (see ml/README.md) —
 * [RiskModel.score] has zero ML runtime dependency, it's just generated arithmetic.
 *
 * Feature order below must exactly match ml/generate_dataset.py's FEATURE_COLUMNS.
 */
object RiskScoringEngine {
    fun score(features: RiskFeatures): RiskAssessment {
        val input = doubleArrayOf(
            features.dailyScreenTimeMin,
            features.avgSessionLengthMin,
            features.appLaunchFrequency,
            features.socialMediaFraction,
            features.bypassAttemptCount,
            features.doomscrollEpisodeCount
        )

        // RiskModel.classes_ is alphabetical: [HIGH, LOW, MODERATE]. Printed by
        // train_risk_model.py at training time — re-verify this if the model is
        // ever retrained with a different label set.
        val probs = RiskModel.score(input)
        val pHigh = probs[0]
        val pLow = probs[1]
        val pModerate = probs[2]

        val level = when {
            pHigh >= pLow && pHigh >= pModerate -> RiskLevel.HIGH
            pModerate >= pLow -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        // Continuous 0.0-1.0 score consistent with MotionInterventionLog.riskScore's
        // existing doc comment: MODERATE counts half, HIGH counts full.
        val continuousScore = (pModerate * 0.5 + pHigh * 1.0).coerceIn(0.0, 1.0)

        return RiskAssessment(level, continuousScore)
    }
}
