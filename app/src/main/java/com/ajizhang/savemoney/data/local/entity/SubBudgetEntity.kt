package com.ajizhang.savemoney.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_budgets",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyBudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["monthlyBudgetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["monthlyBudgetId"])],
)
data class SubBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthlyBudgetId: Long,
    val name: String,
    val amount: Long,
    val createdAt: Long,
)
