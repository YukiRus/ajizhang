package com.ajizhang.savemoney.data.model

data class RecognizedExpenseItem(
    val amountText: String,
    val category: String,
    val note: String,
    val dateText: String,
    val timeText: String,
    val budgetSubName: String = "",
)

data class ImageExpenseRecognitionResult(
    val items: List<RecognizedExpenseItem>,
    val rawResponse: String,
)
