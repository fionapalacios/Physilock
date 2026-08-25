package com.example.physi_lock.ml

import kotlin.math.exp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests ExcessiveUsageDetector's sigmoid + fixed 0.5 threshold against the real generated
 *  ExcessiveUsageModel's output, not the model's own trained accuracy. */
class ExcessiveUsageDetectorTest {

    private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

    private val samples = listOf(
        ExcessiveUsageInputs(avgUsageThisHourMin = 0.0, cumulativeUsageTodayMin = 0.0, usagePrevHourMin = 0.0, isWeekend = false),
        ExcessiveUsageInputs(avgUsageThisHourMin = 45.0, cumulativeUsageTodayMin = 300.0, usagePrevHourMin = 40.0, isWeekend = true),
        ExcessiveUsageInputs(avgUsageThisHourMin = 10.0, cumulativeUsageTodayMin = 60.0, usagePrevHourMin = 5.0, isWeekend = false),
        ExcessiveUsageInputs(avgUsageThisHourMin = 55.0, cumulativeUsageTodayMin = 400.0, usagePrevHourMin = 50.0, isWeekend = true)
    )

    @Test
    fun `predict matches sigmoid of the model logit against the fixed 0point5 threshold`() {
        for (inputs in samples) {
            val logit = ExcessiveUsageModel.score(
                doubleArrayOf(
                    inputs.avgUsageThisHourMin,
                    inputs.cumulativeUsageTodayMin,
                    inputs.usagePrevHourMin,
                    if (inputs.isWeekend) 1.0 else 0.0
                )
            )
            val probability = sigmoid(logit)
            val prediction = ExcessiveUsageDetector.predict(inputs)

            assertEquals("inputs=$inputs", probability >= 0.5, prediction.isExcessive)
            assertEquals("inputs=$inputs", probability, prediction.probability, 1e-9)
        }
    }
}
