package com.ajizhang.savemoney.ui.trend

import com.ajizhang.savemoney.domain.TrendPoint
import com.ajizhang.savemoney.domain.TrendRange
import com.ajizhang.savemoney.ui.home.TransactionSectionGroup
import java.time.LocalDate

enum class TrendSeries {
    INCOME,
    EXPENSE,
    NET,
}

data class TrendUiState(
    val selectedRange: TrendRange = TrendRange.MONTH,
    val selectedSeries: Set<TrendSeries> = TrendSeries.entries.toSet(),
    val excludedExpenseCategories: Set<String> = emptySet(),
    val availableExpenseCategories: List<String> = emptyList(),
    val anchorDate: LocalDate = LocalDate.now(),
    val title: String = "",
    val points: List<TrendPoint> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val totalNet: Long = 0L,
    val hasData: Boolean = false,
    val detailSections: List<TransactionSectionGroup> = emptyList(),
)
