package com.ajizhang.savemoney.domain

import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.DateFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class TrendRange {
    MONTH,
    QUARTER,
    YEAR,
}

data class TrendPoint(
    val label: String,
    val income: Long,
    val expense: Long,
    val net: Long,
)

data class TrendChartData(
    val title: String,
    val points: List<TrendPoint>,
    val totalIncome: Long,
    val totalExpense: Long,
) {
    val totalNet: Long = totalIncome - totalExpense
    val hasData: Boolean = points.any { it.income != 0L || it.expense != 0L || it.net != 0L }
}

object TrendChartCalculator {
    fun buildChart(
        transactions: List<TransactionRecord>,
        range: TrendRange,
        anchorDate: LocalDate,
    ): TrendChartData {
        val buckets = bucketsFor(range, anchorDate)
        val incomeTotals = LongArray(buckets.size)
        val expenseTotals = LongArray(buckets.size)
        val periodStart = buckets.first().start
        val periodEnd = buckets.last().end

        transactions.forEach { transaction ->
            val date = DateFormatter.epochMillisToLocalDate(transaction.occurredAt)
            if (date < periodStart || date > periodEnd) {
                return@forEach
            }

            val index = bucketIndex(range, anchorDate, date)
            if (index !in buckets.indices) {
                return@forEach
            }

            when (transaction.type) {
                TransactionType.INCOME -> incomeTotals[index] += transaction.amount
                TransactionType.EXPENSE -> expenseTotals[index] += transaction.amount - transaction.refundedAmount
            }
        }

        val points = buckets.mapIndexed { index, bucket ->
            val income = incomeTotals[index]
            val expense = expenseTotals[index]
            TrendPoint(
                label = bucket.label,
                income = income,
                expense = expense,
                net = income - expense,
            )
        }

        return TrendChartData(
            title = titleFor(range, anchorDate),
            points = points,
            totalIncome = incomeTotals.sum(),
            totalExpense = expenseTotals.sum(),
        )
    }

    fun move(
        range: TrendRange,
        anchorDate: LocalDate,
        offset: Int,
    ): LocalDate =
        when (range) {
            TrendRange.MONTH -> anchorDate.plusMonths(offset.toLong())
            TrendRange.QUARTER -> anchorDate.plusMonths(offset.toLong() * 3L)
            TrendRange.YEAR -> anchorDate.plusYears(offset.toLong())
        }

    fun titleFor(
        range: TrendRange,
        anchorDate: LocalDate,
    ): String =
        when (range) {
            TrendRange.MONTH -> "${anchorDate.year}年${anchorDate.monthValue}月"
            TrendRange.QUARTER -> "${anchorDate.year}年第${quarterOf(anchorDate)}季度"
            TrendRange.YEAR -> "${anchorDate.year}年"
        }

    private fun bucketsFor(
        range: TrendRange,
        anchorDate: LocalDate,
    ): List<Bucket> =
        when (range) {
            TrendRange.MONTH -> {
                val start = anchorDate.withDayOfMonth(1)
                val end = start.plusMonths(1).minusDays(1)
                generateSequence(start) { current ->
                    current.plusDays(1).takeIf { it <= end }
                }.map { day ->
                    Bucket(
                        start = day,
                        end = day,
                        label = day.dayOfMonth.toString(),
                    )
                }.toList()
            }

            TrendRange.QUARTER -> {
                val quarterStartMonth = ((anchorDate.monthValue - 1) / 3) * 3 + 1
                val quarterStart = LocalDate.of(anchorDate.year, quarterStartMonth, 1)
                val quarterEnd = quarterStart.plusMonths(3).minusDays(1)
                val firstWeekStart = quarterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                buildList {
                    var weekStart = firstWeekStart
                    var index = 1
                    while (weekStart <= quarterEnd) {
                        val weekEnd = weekStart.plusDays(6)
                        if (weekEnd >= quarterStart) {
                            add(
                                Bucket(
                                    start = maxOf(weekStart, quarterStart),
                                    end = minOf(weekEnd, quarterEnd),
                                    label = "${index}周",
                                ),
                            )
                            index += 1
                        }
                        weekStart = weekStart.plusWeeks(1)
                    }
                }
            }

            TrendRange.YEAR -> {
                (1..12).map { month ->
                    val start = LocalDate.of(anchorDate.year, month, 1)
                    Bucket(
                        start = start,
                        end = start.plusMonths(1).minusDays(1),
                        label = "${month}月",
                    )
                }
            }
        }

    private fun bucketIndex(
        range: TrendRange,
        anchorDate: LocalDate,
        date: LocalDate,
    ): Int =
        when (range) {
            TrendRange.MONTH -> date.dayOfMonth - 1
            TrendRange.QUARTER -> {
                val quarterStartMonth = ((anchorDate.monthValue - 1) / 3) * 3 + 1
                val quarterStart = LocalDate.of(anchorDate.year, quarterStartMonth, 1)
                val firstWeekStart = quarterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ChronoUnit.WEEKS.between(firstWeekStart, weekStart).toInt()
            }

            TrendRange.YEAR -> date.monthValue - 1
        }

    private fun quarterOf(date: LocalDate): Int = ((date.monthValue - 1) / 3) + 1

    private data class Bucket(
        val start: LocalDate,
        val end: LocalDate,
        val label: String,
    )
}
