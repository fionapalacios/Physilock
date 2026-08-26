package com.example.physi_lock.sensor

import com.example.physi_lock.ml.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengeSensitivityTest {

    @Test
    fun `fromLabel maps known labels and falls back to MODERATE for anything else`() {
        assertEquals(ChallengeSensitivity.LOW, ChallengeSensitivity.fromLabel("LOW"))
        assertEquals(ChallengeSensitivity.HIGH, ChallengeSensitivity.fromLabel("HIGH"))
        assertEquals(ChallengeSensitivity.MODERATE, ChallengeSensitivity.fromLabel("MODERATE"))
        assertEquals(ChallengeSensitivity.MODERATE, ChallengeSensitivity.fromLabel(null))
        assertEquals(ChallengeSensitivity.MODERATE, ChallengeSensitivity.fromLabel("not-a-real-label"))
    }

    @Test
    fun `fromRiskLevel maps each risk level to the matching sensitivity`() {
        assertEquals(ChallengeSensitivity.LOW, ChallengeSensitivity.fromRiskLevel(RiskLevel.LOW))
        assertEquals(ChallengeSensitivity.MODERATE, ChallengeSensitivity.fromRiskLevel(RiskLevel.MODERATE))
        assertEquals(ChallengeSensitivity.HIGH, ChallengeSensitivity.fromRiskLevel(RiskLevel.HIGH))
    }

    @Test
    fun `displayLabel title-cases every stored value`() {
        assertEquals("Moderate", ChallengeSensitivity.displayLabel("MODERATE"))
        assertEquals("Low", ChallengeSensitivity.displayLabel("LOW"))
        assertEquals("High", ChallengeSensitivity.displayLabel("HIGH"))
    }

    @Test
    fun `higher sensitivity levels require strictly more effort`() {
        // LOW < MODERATE < HIGH across every real challenge requirement -- a regression here
        // would mean, e.g., HIGH accidentally became easier than LOW.
        assertEquals(true, ChallengeSensitivity.LOW.armRepsRequired < ChallengeSensitivity.MODERATE.armRepsRequired)
        assertEquals(true, ChallengeSensitivity.MODERATE.armRepsRequired < ChallengeSensitivity.HIGH.armRepsRequired)
        assertEquals(true, ChallengeSensitivity.LOW.walkStepsRequired < ChallengeSensitivity.MODERATE.walkStepsRequired)
        assertEquals(true, ChallengeSensitivity.MODERATE.walkStepsRequired < ChallengeSensitivity.HIGH.walkStepsRequired)
    }
}
