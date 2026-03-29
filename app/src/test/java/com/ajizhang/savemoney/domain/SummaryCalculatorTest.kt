package com.ajizhang.savemoney.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SummaryCalculatorTest {
    @Test
    fun `deposit equals income minus expense and investment counts into saved`() {
        val summary = SummaryCalculator.calculate(
            targetAmount = 300_000L,
            totalIncome = 200_000L,
            totalExpense = 50_000L,
            investmentAmount = 80_000L,
        )

        assertThat(summary.depositAmount).isEqualTo(150_000L)
        assertThat(summary.savedAmount).isEqualTo(230_000L)
        assertThat(summary.remainingAmount).isEqualTo(70_000L)
    }

    @Test
    fun `remaining amount never drops below zero`() {
        val summary = SummaryCalculator.calculate(
            targetAmount = 100_000L,
            totalIncome = 80_000L,
            totalExpense = 10_000L,
            investmentAmount = 50_000L,
        )

        assertThat(summary.savedAmount).isEqualTo(120_000L)
        assertThat(summary.remainingAmount).isEqualTo(0L)
    }
}
