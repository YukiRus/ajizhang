package com.ajizhang.savemoney.data.model

import com.ajizhang.savemoney.util.MoneyFormatter

data class MonthlyBudget(
    val id: Long = 0,
    val monthKey: String,
    val totalAmount: Long,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val totalAmountText: String get() = MoneyFormatter.format(totalAmount)
}
