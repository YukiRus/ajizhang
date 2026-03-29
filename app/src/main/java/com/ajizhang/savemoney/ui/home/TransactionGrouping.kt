package com.ajizhang.savemoney.ui.home

import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.util.DateFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class TransactionGrouping {
    MONTH,
    QUARTER,
    YEAR,
}

data class TransactionSectionGroup(
    val title: String,
    val transactions: List<TransactionRecord>,
)

object TransactionGroupingHelper {
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月")
    private val yearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年")
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
    private val monthGroupFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月")

    fun currentAnchorDate(): LocalDate = LocalDate.now()

    fun periodLabel(grouping: TransactionGrouping, anchorDate: LocalDate): String =
        when (grouping) {
            TransactionGrouping.MONTH -> anchorDate.format(monthFormatter)
            TransactionGrouping.QUARTER -> "${anchorDate.year}年 第${quarterOf(anchorDate)}季度"
            TransactionGrouping.YEAR -> anchorDate.format(yearFormatter)
        }

    fun move(grouping: TransactionGrouping, anchorDate: LocalDate, step: Long): LocalDate =
        when (grouping) {
            TransactionGrouping.MONTH -> anchorDate.plusMonths(step)
            TransactionGrouping.QUARTER -> anchorDate.plusMonths(step * 3)
            TransactionGrouping.YEAR -> anchorDate.plusYears(step)
        }

    fun buildSections(
        transactions: List<TransactionRecord>,
        grouping: TransactionGrouping,
        anchorDate: LocalDate,
    ): List<TransactionSectionGroup> {
        val filtered = transactions.filter { transaction ->
            val transactionDate = DateFormatter.epochMillisToLocalDate(transaction.occurredAt)
            when (grouping) {
                TransactionGrouping.MONTH ->
                    transactionDate.year == anchorDate.year && transactionDate.month == anchorDate.month

                TransactionGrouping.QUARTER ->
                    transactionDate.year == anchorDate.year && quarterOf(transactionDate) == quarterOf(anchorDate)

                TransactionGrouping.YEAR ->
                    transactionDate.year == anchorDate.year
            }
        }

        return when (grouping) {
            TransactionGrouping.MONTH ->
                filtered.groupBy { DateFormatter.epochMillisToLocalDate(it.occurredAt) }
                    .toSortedMap(compareByDescending { it })
                    .map { (date, items) ->
                        TransactionSectionGroup(
                            title = date.format(dayFormatter),
                            transactions = items,
                        )
                    }

            TransactionGrouping.QUARTER, TransactionGrouping.YEAR ->
                filtered.groupBy { YearMonth.from(DateFormatter.epochMillisToLocalDate(it.occurredAt)) }
                    .toSortedMap(compareByDescending { it })
                    .map { (yearMonth, items) ->
                        TransactionSectionGroup(
                            title = yearMonth.format(monthGroupFormatter),
                            transactions = items,
                        )
                    }
        }
    }

    private fun quarterOf(date: LocalDate): Int = ((date.monthValue - 1) / 3) + 1
}
