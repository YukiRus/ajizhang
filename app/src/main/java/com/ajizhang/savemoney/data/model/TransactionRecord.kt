package com.ajizhang.savemoney.data.model

data class TransactionRecord(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val refundedAmount: Long,
    val category: String,
    val note: String,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
