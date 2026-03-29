package com.ajizhang.savemoney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.repository.GoalRepository
import com.ajizhang.savemoney.data.repository.InvestmentRepository
import com.ajizhang.savemoney.data.repository.TransactionRepository
import com.ajizhang.savemoney.domain.GoalPlanningCalculator
import com.ajizhang.savemoney.domain.SummaryCalculator
import com.ajizhang.savemoney.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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
) : ViewModel() {
    // 首页只消费一个聚合后的状态，数据库字段变化由这里统一折叠成界面模型。
    val uiState: StateFlow<HomeUiState> = combine(
        goalRepository.observeGoal(),
        investmentRepository.observeInvestmentAmount(),
        transactionRepository.observeSummary(),
        transactionRepository.observeTransactions(),
    ) { goal, investmentAmount, summary, transactions ->
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            investmentRepository.ensureInitialized(System.currentTimeMillis())
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
}
