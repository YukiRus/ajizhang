package com.ajizhang.savemoney.data.model

data class ExpenseRecognitionResult(
    val amountText: String,
    val category: String,
    val note: String,
    val dateText: String,
    val timeText: String,
    val budgetSubName: String = "",
    val rawResponse: String,
)
