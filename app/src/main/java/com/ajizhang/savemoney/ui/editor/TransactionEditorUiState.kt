package com.ajizhang.savemoney.ui.editor

import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.DateFormatter

data class TransactionEditorUiState(
    val id: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val refundInput: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val note: String = "",
    val occurredAt: Long = DateFormatter.todayEpochMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isExisting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface TransactionEditorEvent {
    data object Saved : TransactionEditorEvent
    data object Deleted : TransactionEditorEvent
}
