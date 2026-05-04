package com.ajizhang.savemoney.ui.trend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.data.repository.TransactionRepository
import com.ajizhang.savemoney.domain.TrendChartCalculator
import com.ajizhang.savemoney.domain.TrendRange
import com.ajizhang.savemoney.ui.home.TransactionGrouping
import com.ajizhang.savemoney.ui.home.TransactionGroupingHelper
import com.ajizhang.savemoney.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TrendViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
) : ViewModel() {
    private val selectedRange = MutableStateFlow(TrendRange.MONTH)
    private val selectedSeries = MutableStateFlow(TrendSeries.entries.toSet())
    private val excludedExpenseCategories = MutableStateFlow(emptySet<String>())
    private val anchorDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TrendUiState> = combine(
        transactionRepository.observeTransactions(),
        selectedRange,
        selectedSeries,
        excludedExpenseCategories,
        anchorDate,
    ) { transactions, range, series, excludedCategories, date ->
        val availableExpenseCategories = transactions
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                    transaction.isInRange(range = range, anchorDate = date)
            }
            .map { it.category }
            .distinct()
            .sorted()
        val filteredTransactions = transactions.filterNot { transaction ->
            transaction.type == TransactionType.EXPENSE && transaction.category in excludedCategories
        }
        val chart = TrendChartCalculator.buildChart(
            transactions = filteredTransactions,
            range = range,
            anchorDate = date,
        )
        val detailSections = TransactionGroupingHelper.buildSections(
            transactions = filteredTransactions,
            grouping = range.toTransactionGrouping(),
            anchorDate = date,
        )
        TrendUiState(
            selectedRange = range,
            selectedSeries = series,
            excludedExpenseCategories = excludedCategories,
            availableExpenseCategories = availableExpenseCategories,
            anchorDate = date,
            title = chart.title,
            points = chart.points,
            totalIncome = chart.totalIncome,
            totalExpense = chart.totalExpense,
            totalNet = chart.totalNet,
            hasData = chart.hasData,
            detailSections = detailSections,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendUiState(),
    )

    fun selectRange(range: TrendRange) {
        selectedRange.value = range
    }

    fun toggleSeries(series: TrendSeries) {
        selectedSeries.value = selectedSeries.value.toggle(series)
    }

    fun toggleExpenseCategory(category: String) {
        excludedExpenseCategories.value =
            if (category in excludedExpenseCategories.value) {
                excludedExpenseCategories.value - category
            } else {
                excludedExpenseCategories.value + category
            }
    }

    fun clearExpenseCategoryFilters() {
        excludedExpenseCategories.value = emptySet()
    }

    fun move(offset: Int) {
        anchorDate.value = TrendChartCalculator.move(
            range = selectedRange.value,
            anchorDate = anchorDate.value,
            offset = offset,
        )
    }

    private fun Set<TrendSeries>.toggle(series: TrendSeries): Set<TrendSeries> =
        if (series in this && size > 1) {
            this - series
        } else if (series in this) {
            this
        } else {
            this + series
        }

    private fun TrendRange.toTransactionGrouping(): TransactionGrouping =
        when (this) {
            TrendRange.MONTH -> TransactionGrouping.MONTH
            TrendRange.QUARTER -> TransactionGrouping.QUARTER
            TrendRange.YEAR -> TransactionGrouping.YEAR
        }

    private fun TransactionRecord.isInRange(
        range: TrendRange,
        anchorDate: LocalDate,
    ): Boolean {
        val transactionDate = DateFormatter.epochMillisToLocalDate(occurredAt)
        return when (range) {
            TrendRange.MONTH ->
                transactionDate.year == anchorDate.year && transactionDate.month == anchorDate.month

            TrendRange.QUARTER ->
                transactionDate.year == anchorDate.year && quarterOf(transactionDate) == quarterOf(anchorDate)

            TrendRange.YEAR ->
                transactionDate.year == anchorDate.year
        }
    }

    private fun quarterOf(date: LocalDate): Int = ((date.monthValue - 1) / 3) + 1
}
