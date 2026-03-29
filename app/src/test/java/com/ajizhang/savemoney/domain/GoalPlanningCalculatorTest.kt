package com.ajizhang.savemoney.domain

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class GoalPlanningCalculatorTest {
    @Test
    fun `recommendation spreads remaining amount across current month to target month`() {
        val plan = GoalPlanningCalculator.calculate(
            remainingAmount = 90_000L,
            today = LocalDate.of(2026, 3, 30),
            expectedDate = LocalDate.of(2026, 5, 20),
        )

        assertThat(plan.recommendedMonthlyAmount).isEqualTo(30_000L)
        assertThat(plan.isDeadlinePassed).isFalse()
    }

    @Test
    fun `expired goal date returns passed state without recommendation`() {
        val plan = GoalPlanningCalculator.calculate(
            remainingAmount = 90_000L,
            today = LocalDate.of(2026, 3, 30),
            expectedDate = LocalDate.of(2026, 3, 1),
        )

        assertThat(plan.recommendedMonthlyAmount).isNull()
        assertThat(plan.isDeadlinePassed).isTrue()
    }
}
