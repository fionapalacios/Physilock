package com.example.physi_lock.ml

import kotlin.math.exp

data class DoomscrollInputs(
    val scrollSpeedPerMin: Double,
    val maxPauseGapSec: Double,
    val hourOfDay: Double
)

/**
 * Per-risk-level detection threshold ("recipe") the 3 raw scroll inputs are
 * evaluated against. Confirmed design (project memory "module2-doomscroll-
 * design"): the behavioral risk score is NOT a 4th model input — it selects
 * which threshold applies to the model's output, so a high-risk user gets
 * flagged by a weaker doomscroll signal than a low-risk user would need.
 */
private val THRESHOLD_BY_RISK_LEVEL = mapOf(
    RiskLevel.LOW to 0.75,
    RiskLevel.MODERATE to 0.60,
    RiskLevel.HIGH to 0.45
)

/**
 * Module 2 (AI-Based Behavior Analysis): Logistic Regression I, trained offline
 * in ml/train_doomscroll_model.py and transpiled via m2cgen (see ml/README.md).
 *
 * Unlike RiskModel's RandomForestClassifier export, m2cgen's LogisticRegression
 * Java export returns a raw logit (the linear combination, pre-sigmoid) as a
 * single double — not a probability array — so the sigmoid is applied here.
 *
 * Feature order below must exactly match ml/generate_doomscroll_dataset.py's
 * FEATURE_COLUMNS: [scrollSpeedPerMin, maxPauseGapSec, hourOfDay].
 */
object DoomscrollDetector {
    fun detect(inputs: DoomscrollInputs, riskLevel: RiskLevel): Boolean {
        val input = doubleArrayOf(inputs.scrollSpeedPerMin, inputs.maxPauseGapSec, inputs.hourOfDay)
        val logit = DoomscrollModel.score(input)
        val probability = sigmoid(logit)
        val threshold = THRESHOLD_BY_RISK_LEVEL.getValue(riskLevel)
        return probability >= threshold
    }

    private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))
}
