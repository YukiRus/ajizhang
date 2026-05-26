package com.ajizhang.savemoney.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_budgets",
    indices = [Index(value = ["monthKey"], unique = true)],
)
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val totalAmount: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
