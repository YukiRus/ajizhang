package com.ajizhang.savemoney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajizhang.savemoney.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val refundedAmount: Long,
    val category: String,
    val note: String,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val subBudgetId: Long? = null,
)
