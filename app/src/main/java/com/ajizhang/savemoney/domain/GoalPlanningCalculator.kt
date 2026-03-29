package com.ajizhang.savemoney.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GoalPlan(
    val recommendedMonthlyAmount: Long?,
    val isDeadlinePassed: Boolean,
)

object GoalPlanningCalculator {
    fun empty(): GoalPlan = GoalPlan(recommendedMonthlyAmount = null, isDeadlinePassed = false)

    // 按“当前月份到目标月份（含目标月份）”均摊剩余金额，给首页一个可执行的月度建议。
    fun calculate(
        remainingAmount: Long?,
        today: LocalDate,
        expectedDate: LocalDate,
    ): GoalPlan {
        if (remainingAmount == null || remainingAmount <= 0L) {
            return empty()
        }
        if (expectedDate.isBefore(today)) {
            return GoalPlan(recommendedMonthlyAmount = null, isDeadlinePassed = true)
        }

        val months =
            ChronoUnit.MONTHS.between(
                today.withDayOfMonth(1),
                expectedDate.withDayOfMonth(1),
            ) + 1
        val safeMonths = months.coerceAtLeast(1L)
        val recommended = (remainingAmount + safeMonths - 1L) / safeMonths

        return GoalPlan(
            recommendedMonthlyAmount = recommended,
            isDeadlinePassed = false,
        )
    }
}
