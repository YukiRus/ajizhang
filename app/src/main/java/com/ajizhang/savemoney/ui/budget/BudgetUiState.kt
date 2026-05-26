package com.ajizhang.savemoney.ui.budget

import com.ajizhang.savemoney.data.model.MonthlyBudget
import com.ajizhang.savemoney.data.model.SubBudget
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.util.DateFormatter

data class BudgetUiState(
    val monthKey: String = DateFormatter.currentMonthKey(),
    val monthlyBudget: MonthlyBudget? = null,
    val subBudgets: List<SubBudget> = emptyList(),
    val totalBudgetInput: String = "",
    val totalSubBudgetSum: Long = 0,
    val totalSpent: Long = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val detailSubBudget: SubBudget? = null,
    val detailTransactions: List<TransactionRecord> = emptyList(),
    val isDetailLoading: Boolean = false,
)
