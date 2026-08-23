package com.example.physi_lock.ml

import kotlin.math.exp

data class ExcessiveUsageInputs(
    val avgUsageThisHourMin: Double,
    val cumulativeUsageTodayMin: Double,
    val usagePrevHourMin: Double,
    val isWeekend: Boolean
)

data class ExcessiveUsagePrediction(val isExcessive: Boolean, val probability: Double)

/**
 * Module 2 (AI-Based Behavior Analysis): Logistic Regression II, trained offline in
 * ml/train_excessive_usage_model.py and transpiled via m2cgen (see ml/README.md).
 *
 * The manuscript's Data Dictionary only specifies this model's *output* schema
 * (EXCESSIVE_USAGE_PREDICTION table: hour, predicted_usage_minutes,
 * excessive_probability, is_excessive) — it doesn't enumerate input features, so
 * this 4-feature set was proposed and confirmed separately (2026-08-23).
 *
 * Like DoomscrollModel, ExcessiveUsageModel.score() returns a raw logit
 * (pre-sigmoid), not a probability — sigmoid is applied here. Unlike
 * DoomscrollDetector, there's no risk-level threshold "recipe" for this model —
 * that pattern was specifically confirmed for the doomscroll classifier only.
 *
 * Feature order below must exactly match
 * ml/generate_excessive_usage_dataset.py's FEATURE_COLUMNS.
 */
object ExcessiveUsageDetector {
    private const val THRESHOLD = 0.5

    fun predict(inputs: ExcessiveUsageInputs): ExcessiveUsagePrediction {
        val input = doubleArrayOf(
            inputs.avgUsageThisHourMin,
            inputs.cumulativeUsageTodayMin,
            inputs.usagePrevHourMin,
            if (inputs.isWeekend) 1.0 else 0.0
        )
        val logit = ExcessiveUsageModel.score(input)
        val probability = 1.0 / (1.0 + exp(-logit))
        return ExcessiveUsagePrediction(probability >= THRESHOLD, probability)
    }
}
