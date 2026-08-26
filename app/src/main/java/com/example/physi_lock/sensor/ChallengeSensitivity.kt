package com.example.physi_lock.sensor

import com.example.physi_lock.ml.RiskLevel

// Mirrors UserConfiguration.motionLockSensitivity ("LOW"/"MODERATE"/"HIGH") — the
// single source of truth for how the setting maps to detector behavior, shared
// between the actual challenges (RotationalArmDetector/StepChallengeDetector)
// and their display in Settings/Admin defaults. Replaces the old ShakeSensitivity.
enum class ChallengeSensitivity(
    val armRepsRequired: Int,
    val armGForceThreshold: Float,
    val walkStepsRequired: Int,
    val jogDurationMs: Long,
    val jogMinStepsPerMin: Int
) {
    LOW(4, 18f, 60, 2 * 60_000L, 110),
    MODERATE(6, 23.5f, 100, 3 * 60_000L, 130),
    HIGH(9, 28f, 150, 3 * 60_000L, 150);

    companion object {
        fun fromLabel(label: String?): ChallengeSensitivity = when (label) {
            "LOW" -> LOW
            "HIGH" -> HIGH
            else -> MODERATE
        }

        // Trigger Adaptive Lock (Module 3, unblocked by Module 2's real risk score):
        // a higher behavioral risk level makes the unlock challenge harder, rather
        // than difficulty being a static Settings choice. Used for AppLockRules with
        // lockType == "ADAPTIVE" — see LockScreen.kt.
        fun fromRiskLevel(level: RiskLevel): ChallengeSensitivity = when (level) {
            RiskLevel.LOW -> LOW
            RiskLevel.MODERATE -> MODERATE
            RiskLevel.HIGH -> HIGH
        }

        // Every stored/compared sensitivity value (UserConfiguration rows,
        // AdminDefaultsSection, etc.) already reads "LOW"/"MODERATE"/"HIGH" — the display
        // label is just each value title-cased, no special-casing needed.
        fun displayLabel(label: String): String = label.lowercase().replaceFirstChar { it.uppercase() }
    }
}
