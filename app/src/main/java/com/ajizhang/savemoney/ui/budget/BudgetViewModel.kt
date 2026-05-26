package com.ajizhang.savemoney.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.model.MonthlyBudget
import com.ajizhang.savemoney.data.model.SubBudget
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.repository.BudgetRepository
import com.ajizhang.savemoney.data.repository.TransactionRepository
import com.ajizhang.savemoney.util.DateFormatter
import com.ajizhang.savemoney.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val monthKey = MutableStateFlow(DateFormatter.currentMonthKey())

    val uiState: StateFlow<BudgetUiState> = monthKey
        .flatMapLatest { key ->
            combine(
                budgetRepository.observeBudgetByMonth(key),
                budgetRepository.observeAllBudgets(),
            ) { budget, allBudgets ->
                key to (budget to allBudgets)
            }
        }
        .flatMapLatest { (key, pair) ->
            val (budget, allBudgets) = pair
            val subBudgetsFlow = if (budget != null) {
                budgetRepository.observeSubBudgets(budget.id)
            } else {
                flowOf(emptyList())
            }
            subBudgetsFlow.map { subBudgets ->
                val totalSubBudgetSum = subBudgets.sumOf { it.amount }
                val totalSpent = subBudgets.sumOf { it.spent }
                BudgetUiState(
                    monthKey = key,
                    monthlyBudget = budget,
                    subBudgets = subBudgets,
                    totalBudgetInput = budget?.totalAmount?.let(MoneyFormatter::toInputValue) ?: "",
                    totalSubBudgetSum = totalSubBudgetSum,
                    totalSpent = totalSpent,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState(),
        )

    private val _detailState = MutableStateFlow(SubBudgetDetail())
    val detailState: StateFlow<SubBudgetDetail> = _detailState.asStateFlow()

    fun setMonthKey(key: String) {
        monthKey.value = key
    }

    fun goToPreviousMonth() {
        monthKey.value = shiftMonthKey(monthKey.value, -1)
    }

    fun goToNextMonth() {
        monthKey.value = shiftMonthKey(monthKey.value, 1)
    }

    fun saveMainBudget(amountInput: String) {
        val amount = MoneyFormatter.parseToCents(amountInput)
        if (amount == null || amount <= 0L) {
            return
        }
        val key = monthKey.value
        viewModelScope.launch {
            budgetRepository.saveBudget(key, amount)
        }
    }

    fun addSubBudget(name: String, amountInput: String): String? {
        val amount = MoneyFormatter.parseToCents(amountInput) ?: return "请输入有效金额"
        if (amount <= 0L || name.isBlank()) return "请输入名称和金额"

        val state = uiState.value
        val budget = state.monthlyBudget ?: return "请先设置当月总预算"

        val newTotal = state.totalSubBudgetSum + amount
        if (newTotal > budget.totalAmount) {
            return "子预算总计(${MoneyFormatter.format(newTotal)})将超过主预算(${MoneyFormatter.format(budget.totalAmount)})"
        }

        viewModelScope.launch {
            budgetRepository.addSubBudget(budget.id, name.trim(), amount)
        }
        return null
    }

    fun updateSubBudget(id: Long, name: String, amountInput: String): String? {
        val amount = MoneyFormatter.parseToCents(amountInput) ?: return "请输入有效金额"
        if (amount <= 0L || name.isBlank()) return "请输入名称和金额"

        val state = uiState.value
        val budget = state.monthlyBudget ?: return "未找到预算"
        val oldSubBudget = state.subBudgets.find { it.id == id } ?: return "未找到子预算"

        val newTotal = state.totalSubBudgetSum - oldSubBudget.amount + amount
        if (newTotal > budget.totalAmount) {
            return "子预算总计(${MoneyFormatter.format(newTotal)})将超过主预算(${MoneyFormatter.format(budget.totalAmount)})"
        }

        viewModelScope.launch {
            budgetRepository.updateSubBudget(id, name.trim(), amount)
        }
        return null
    }

    fun deleteSubBudget(id: Long) {
        viewModelScope.launch {
            budgetRepository.deleteSubBudget(id)
        }
    }

    fun copyToMonth(targetMonthKey: String) {
        viewModelScope.launch {
            budgetRepository.copySubBudgets(monthKey.value, targetMonthKey)
        }
    }

    fun getMonthKeysForDropdown(): List<String> =
        buildList {
            val key = monthKey.value
            for (i in -12..12) {
                val candidate = shiftMonthKey(key, i)
                if (candidate != key) {
                    add(candidate)
                }
            }
            sort()
        }

    private fun shiftMonthKey(key: String, delta: Int): String {
        val parts = key.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val totalMonths = year * 12 + (month - 1) + delta
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        return "%04d-%02d".format(newYear, newMonth)
    }

    fun openSubBudgetDetail(subBudget: SubBudget) {
        viewModelScope.launch {
            _detailState.value = SubBudgetDetail(
                subBudget = subBudget,
                transactions = emptyList(),
                isLoading = true,
            )
            val transactions = transactionRepository.getTransactionsBySubBudgetId(subBudget.id)
            _detailState.value = SubBudgetDetail(
                subBudget = subBudget,
                transactions = transactions,
                isLoading = false,
            )
        }
    }

    fun closeSubBudgetDetail() {
        _detailState.value = SubBudgetDetail()
    }
}

data class SubBudgetDetail(
    val subBudget: SubBudget? = null,
    val transactions: List<TransactionRecord> = emptyList(),
    val isLoading: Boolean = false,
)
