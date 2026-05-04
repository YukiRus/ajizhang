package com.ajizhang.savemoney.domain

import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.DateFormatter
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class TrendChartCalculatorTest {
    @Test
    fun `month view aggregates by day and fills missing dates`() {
        val chart = TrendChartCalculator.buildChart(
            transactions = listOf(
                transaction(date = LocalDate.of(2026, 4, 2), type = TransactionType.INCOME, amount = 10_000L),
                transaction(date = LocalDate.of(2026, 4, 2), type = TransactionType.EXPENSE, amount = 4_000L),
                transaction(date = LocalDate.of(2026, 4, 15), type = TransactionType.EXPENSE, amount = 3_000L),
            ),
            range = TrendRange.MONTH,
            anchorDate = LocalDate.of(2026, 4, 10),
        )

        assertThat(chart.points).hasSize(30)
        assertThat(chart.points[1].label).isEqualTo("2")
        assertThat(chart.points[1].income).isEqualTo(10_000L)
        assertThat(chart.points[1].expense).isEqualTo(4_000L)
        assertThat(chart.points[1].net).isEqualTo(6_000L)
        assertThat(chart.points[9].income).isEqualTo(0L)
        assertThat(chart.points[14].expense).isEqualTo(3_000L)
    }

    @Test
    fun `quarter view groups transactions into monday based weeks`() {
        val chart = TrendChartCalculator.buildChart(
            transactions = listOf(
                transaction(date = LocalDate.of(2026, 3, 31), type = TransactionType.INCOME, amount = 9_000L),
                transaction(date = LocalDate.of(2026, 4, 1), type = TransactionType.INCOME, amount = 10_000L),
                transaction(date = LocalDate.of(2026, 4, 5), type = TransactionType.EXPENSE, amount = 2_000L),
                transaction(date = LocalDate.of(2026, 4, 7), type = TransactionType.EXPENSE, amount = 1_500L),
                transaction(date = LocalDate.of(2026, 6, 30), type = TransactionType.INCOME, amount = 8_000L),
            ),
            range = TrendRange.QUARTER,
            anchorDate = LocalDate.of(2026, 5, 20),
        )

        assertThat(chart.points).hasSize(14)
        assertThat(chart.points.first().label).isEqualTo("1周")
        assertThat(chart.points.first().income).isEqualTo(10_000L)
        assertThat(chart.points.first().expense).isEqualTo(2_000L)
        assertThat(chart.points[1].expense).isEqualTo(1_500L)
        assertThat(chart.points.last().income).isEqualTo(8_000L)
        assertThat(chart.totalIncome).isEqualTo(18_000L)
    }

    @Test
    fun `year view groups transactions by month and fills all months`() {
        val chart = TrendChartCalculator.buildChart(
            transactions = listOf(
                transaction(date = LocalDate.of(2026, 1, 8), type = TransactionType.INCOME, amount = 12_000L),
                transaction(date = LocalDate.of(2026, 8, 9), type = TransactionType.EXPENSE, amount = 5_000L),
            ),
            range = TrendRange.YEAR,
            anchorDate = LocalDate.of(2026, 11, 1),
        )

        assertThat(chart.points).hasSize(12)
        assertThat(chart.points[0].label).isEqualTo("1月")
        assertThat(chart.points[0].income).isEqualTo(12_000L)
        assertThat(chart.points[7].expense).isEqualTo(5_000L)
        assertThat(chart.points[5].net).isEqualTo(0L)
    }

    @Test
    fun `refund reduces expense and adjusts net value`() {
        val chart = TrendChartCalculator.buildChart(
            transactions = listOf(
                transaction(
                    date = LocalDate.of(2026, 4, 12),
                    type = TransactionType.EXPENSE,
                    amount = 8_000L,
                    refundedAmount = 3_000L,
                ),
            ),
            range = TrendRange.MONTH,
            anchorDate = LocalDate.of(2026, 4, 1),
        )

        assertThat(chart.points[11].expense).isEqualTo(5_000L)
        assertThat(chart.points[11].net).isEqualTo(-5_000L)
        assertThat(chart.totalExpense).isEqualTo(5_000L)
    }

    private fun transaction(
        date: LocalDate,
        type: TransactionType,
        amount: Long,
        refundedAmount: Long = 0L,
    ): TransactionRecord =
        TransactionRecord(
            id = 0L,
            type = type,
            amount = amount,
            refundedAmount = refundedAmount,
            category = "测试",
            note = "",
            occurredAt = DateFormatter.localDateToEpochMillis(date),
            createdAt = 0L,
            updatedAt = 0L,
        )
}
