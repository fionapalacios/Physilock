package com.example.physi_lock.sensor

import com.example.physi_lock.ml.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengeSensitivityTest {

    @Test
    fun `fromLabel maps known labels and falls back to MEDIUM for anything else`() {
        assertEquals(ChallengeSensitivity.LOW, ChallengeSensitivity.fromLabel("LOW"))
        assertEquals(ChallengeSensitivity.HIGH, ChallengeSensitivity.fromLabel("HIGH"))
        assertEquals(ChallengeSensitivity.MEDIUM, ChallengeSensitivity.fromLabel("MEDIUM"))
        assertEquals(ChallengeSensitivity.MEDIUM, ChallengeSensitivity.fromLabel(null))
        assertEquals(ChallengeSensitivity.MEDIUM, ChallengeSensitivity.fromLabel("not-a-real-label"))
    }

    @Test
    fun `fromRiskLevel maps each risk level to the matching sensitivity`() {
        assertEquals(ChallengeSensitivity.LOW, ChallengeSensitivity.fromRiskLevel(RiskLevel.LOW))
        assertEquals(ChallengeSensitivity.MEDIUM, ChallengeSensitivity.fromRiskLevel(RiskLevel.MODERATE))
        assertEquals(ChallengeSensitivity.HIGH, ChallengeSensitivity.fromRiskLevel(RiskLevel.HIGH))
    }

    @Test
    fun `displayLabel renames MEDIUM to Moderate but title-cases everything else`() {
        assertEquals("Moderate", ChallengeSensitivity.displayLabel("MEDIUM"))
        assertEquals("Low", ChallengeSensitivity.displayLabel("LOW"))
        assertEquals("High", ChallengeSensitivity.displayLabel("HIGH"))
    }

    @Test
    fun `higher sensitivity levels require strictly more effort`() {
        // LOW < MEDIUM < HIGH across every real challenge requirement -- a regression here
        // would mean, e.g., HIGH accidentally became easier than LOW.
        assertEquals(true, ChallengeSensitivity.LOW.armRepsRequired < ChallengeSensitivity.MEDIUM.armRepsRequired)
        assertEquals(true, ChallengeSensitivity.MEDIUM.armRepsRequired < ChallengeSensitivity.HIGH.armRepsRequired)
        assertEquals(true, ChallengeSensitivity.LOW.walkStepsRequired < ChallengeSensitivity.MEDIUM.walkStepsRequired)
        assertEquals(true, ChallengeSensitivity.MEDIUM.walkStepsRequired < ChallengeSensitivity.HIGH.walkStepsRequired)
    }
}
