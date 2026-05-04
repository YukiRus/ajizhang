package com.ajizhang.savemoney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.model.RecognizedExpenseItem
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.data.repository.CategoryRepository
import com.ajizhang.savemoney.data.repository.GoalRepository
import com.ajizhang.savemoney.data.repository.InvestmentRepository
import com.ajizhang.savemoney.data.repository.TransactionRepository
import com.ajizhang.savemoney.domain.GoalPlanningCalculator
import com.ajizhang.savemoney.domain.SummaryCalculator
import com.ajizhang.savemoney.util.DateFormatter
import com.ajizhang.savemoney.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val investmentRepository: InvestmentRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    // 首页只消费一个聚合后的状态，数据库字段变化由这里统一折叠成界面模型。
    val uiState: StateFlow<HomeUiState> = combine(
        goalRepository.observeGoal(),
        investmentRepository.observeInvestmentAmount(),
        transactionRepository.observeSummary(),
        transactionRepository.observeTransactions(),
        categoryRepository.observeAllCategories(),
    ) { goal, investmentAmount, summary, transactions, categoryMap ->
        val calculated = SummaryCalculator.calculate(
            targetAmount = goal?.targetAmount,
            totalIncome = summary.totalIncome,
            totalExpense = summary.totalExpense,
            investmentAmount = investmentAmount,
        )
        val plan = goal?.expectedDate?.let { expectedDate ->
            GoalPlanningCalculator.calculate(
                remainingAmount = calculated.remainingAmount,
                today = LocalDate.now(),
                expectedDate = DateFormatter.epochMillisToLocalDate(expectedDate),
            )
        } ?: GoalPlanningCalculator.empty()

        HomeUiState(
            goalName = goal?.name.orEmpty(),
            targetAmount = goal?.targetAmount,
            expectedDate = goal?.expectedDate,
            investmentAmount = investmentAmount,
            depositAmount = calculated.depositAmount,
            savedAmount = calculated.savedAmount,
            remainingAmount = calculated.remainingAmount,
            recommendedMonthlyAmount = plan.recommendedMonthlyAmount,
            isExpectedDatePassed = plan.isDeadlinePassed,
            transactions = transactions,
            expenseCategories = categoryMap[TransactionType.EXPENSE].orEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            investmentRepository.ensureInitialized(System.currentTimeMillis())
            categoryRepository.ensureDefaults()
        }
    }

    fun saveGoal(name: String, targetAmount: Long, expectedDate: Long?) {
        viewModelScope.launch {
            goalRepository.saveGoal(
                name = name,
                targetAmount = targetAmount,
                expectedDate = expectedDate,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    fun saveInvestment(amount: Long) {
        viewModelScope.launch {
            investmentRepository.saveInvestmentAmount(
                amount = amount,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    fun saveRecognizedExpenses(items: List<RecognizedExpenseItem>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            items.forEach { item ->
                val amountInCents = MoneyFormatter.parseToCents(item.amountText) ?: return@forEach
                if (amountInCents <= 0L) return@forEach

                val localDate = runCatching { LocalDate.parse(item.dateText.trim()) }
                    .getOrDefault(LocalDate.now())
                val localTime = runCatching { LocalTime.parse(item.timeText.trim()) }
                    .getOrDefault(LocalTime.MIDNIGHT)
                val occurredAt = DateFormatter.localDateTimeToEpochMillis(
                    LocalDateTime.of(localDate, localTime),
                )

                transactionRepository.saveTransaction(
                    id = 0L,
                    type = TransactionType.EXPENSE,
                    amount = amountInCents,
                    refundedAmount = 0L,
                    category = item.category.ifBlank { "其他" },
                    note = item.note.trim(),
                    occurredAt = occurredAt,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
    }
}
