package com.ajizhang.savemoney.ui.home

import com.ajizhang.savemoney.data.model.TransactionRecord

data class HomeUiState(
    val goalName: String = "",
    val targetAmount: Long? = null,
    val expectedDate: Long? = null,
    val investmentAmount: Long = 0L,
    val depositAmount: Long = 0L,
    val savedAmount: Long = 0L,
    val remainingAmount: Long? = null,
    val recommendedMonthlyAmount: Long? = null,
    val isExpectedDatePassed: Boolean = false,
    val transactions: List<TransactionRecord> = emptyList(),
    val expenseCategories: List<String> = emptyList(),
) {
    val hasGoal: Boolean
        get() = targetAmount != null
}
