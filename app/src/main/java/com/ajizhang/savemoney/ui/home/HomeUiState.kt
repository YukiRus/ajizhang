package com.ajizhang.savemoney.ui.home

import com.ajizhang.savemoney.data.model.TransactionRecord

data class HomeUiState(
    val goalName: String = "",
    val targetAmount: Long? = null,
    val investmentAmount: Long = 0L,
    val depositAmount: Long = 0L,
    val savedAmount: Long = 0L,
    val remainingAmount: Long? = null,
    val transactions: List<TransactionRecord> = emptyList(),
) {
    val hasGoal: Boolean
        get() = targetAmount != null
}
