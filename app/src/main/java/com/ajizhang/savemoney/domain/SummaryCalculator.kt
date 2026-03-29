package com.ajizhang.savemoney.domain

data class SavingsSummary(
    val depositAmount: Long,
    val savedAmount: Long,
    val remainingAmount: Long?,
)

object SummaryCalculator {
    // App 的核心统计口径集中在这里，避免 UI 和 Repository 各自重复计算。
    fun calculate(
        targetAmount: Long?,
        totalIncome: Long,
        totalExpense: Long,
        investmentAmount: Long,
    ): SavingsSummary {
        val depositAmount = totalIncome - totalExpense
        val savedAmount = depositAmount + investmentAmount
        val remainingAmount = targetAmount?.let { maxOf(it - savedAmount, 0L) }

        return SavingsSummary(
            depositAmount = depositAmount,
            savedAmount = savedAmount,
            remainingAmount = remainingAmount,
        )
    }
}
