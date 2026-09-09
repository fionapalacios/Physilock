package com.example.physi_lock.sensor

// 2026-09-09: ROTATIONAL_ARM split into 6 named arm-exercise variants -- one generic
// "rotate your arm" challenge couldn't tell a deliberate swing from incidental sway.
// Each variant gets its own instruction copy (LockScreen/MoveScreen/
// ChallengeActiveScreen) and its own motion-intensity threshold tier in
// RotationalArmRepConfig (vigorous swing vs. gentle sway/stretch) -- not real
// per-exercise shape detection (the phone can't tell a curl from a side raise without
// knowing how it's held; see RotationalArmRepConfig's doc comment). XP scaled by
// effort: vigorous variants match the old ROTATIONAL_ARM value, curl and the gentle
// variants (sway/stretch) less.
enum class ChallengeType(val xpReward: Int) {
    RUN_JOG(40),
    WALK(20),
    ARM_SWING_FRONT_BACK(15),
    ARM_FULL_ROTATION(15),
    ARM_SIDE_RAISE(15),
    ARM_BICEP_CURL(13),
    ARM_SWAY(10),
    ARM_STRETCH(10);

    companion object {
        val ARM_VARIANTS = listOf(
            ARM_SWING_FRONT_BACK,
            ARM_FULL_ROTATION,
            ARM_SIDE_RAISE,
            ARM_BICEP_CURL,
            ARM_SWAY,
            ARM_STRETCH
        )
    }
}
