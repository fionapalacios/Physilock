package com.example.physi_lock.sensor

// Mirrors UserConfiguration.motionLockSensitivity ("LOW"/"MEDIUM"/"HIGH") — the
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
    MEDIUM(6, 23.5f, 100, 3 * 60_000L, 130),
    HIGH(9, 28f, 150, 3 * 60_000L, 150);

    companion object {
        fun fromLabel(label: String?): ChallengeSensitivity = when (label) {
            "LOW" -> LOW
            "HIGH" -> HIGH
            else -> MEDIUM
        }
    }
}
