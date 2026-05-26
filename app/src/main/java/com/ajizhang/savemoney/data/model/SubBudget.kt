package com.ajizhang.savemoney.data.model

import com.ajizhang.savemoney.util.MoneyFormatter

data class SubBudget(
    val id: Long = 0,
    val monthlyBudgetId: Long,
    val name: String,
    val amount: Long,
    val spent: Long = 0,
    val createdAt: Long,
) {
    val amountText: String get() = MoneyFormatter.format(amount)
    val spentText: String get() = MoneyFormatter.format(spent)
    val remaining: Long get() = (amount - spent).coerceAtLeast(0)
    val remainingText: String get() = MoneyFormatter.format(remaining)
}
