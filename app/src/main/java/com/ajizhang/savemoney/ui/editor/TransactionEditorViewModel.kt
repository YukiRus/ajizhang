package com.ajizhang.savemoney.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.data.repository.CategoryRepository
import com.ajizhang.savemoney.data.repository.TransactionRepository
import com.ajizhang.savemoney.ui.navigation.Routes
import com.ajizhang.savemoney.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val transactionId: Long =
        savedStateHandle.get<Long>(Routes.ARG_TRANSACTION_ID) ?: Routes.NEW_TRANSACTION_ID

    private val formState = MutableStateFlow(TransactionEditorUiState())
    val uiState: StateFlow<TransactionEditorUiState> = combine(
        formState,
        categoryRepository.observeAllCategories(),
    ) { state, categoryMap ->
        val managedCategories = categoryMap[state.type].orEmpty()
        val resolvedCategory = when {
            state.category in managedCategories -> state.category
            state.isExisting && state.category.isNotBlank() -> state.category
            managedCategories.isNotEmpty() -> managedCategories.first()
            else -> ""
        }
        val visibleCategories =
            if (resolvedCategory.isNotBlank() && resolvedCategory !in managedCategories) {
                listOf(resolvedCategory) + managedCategories
            } else {
                managedCategories
            }

        state.copy(
            category = resolvedCategory,
            categories = visibleCategories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionEditorUiState(),
    )

    private val _events = MutableSharedFlow<TransactionEditorEvent>()
    val events: SharedFlow<TransactionEditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaults()
        }
        if (transactionId == Routes.NEW_TRANSACTION_ID) {
            formState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                val transaction = transactionRepository.getTransaction(transactionId)
                formState.update {
                    if (transaction == null) {
                        it.copy(isLoading = false, errorMessage = "未找到这条记录")
                    } else {
                        it.copy(
                            id = transaction.id,
                            type = transaction.type,
                            amountInput = MoneyFormatter.toInputValue(transaction.amount),
                            refundInput = MoneyFormatter.toInputValue(transaction.refundedAmount),
                            category = transaction.category,
                            note = transaction.note,
                            occurredAt = transaction.occurredAt,
                            createdAt = transaction.createdAt,
                            isExisting = true,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        formState.update { state ->
            state.copy(
                type = type,
                category = "",
                refundInput = if (type == TransactionType.EXPENSE) state.refundInput else "",
                errorMessage = null,
            )
        }
    }

    fun onAmountChange(amountInput: String) {
        formState.update { it.copy(amountInput = amountInput, errorMessage = null) }
    }

    fun onCategoryChange(category: String) {
        formState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onRefundChange(refundInput: String) {
        formState.update { it.copy(refundInput = refundInput, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        formState.update { it.copy(note = note, errorMessage = null) }
    }

    fun onDateChange(occurredAt: Long) {
        formState.update { it.copy(occurredAt = occurredAt, errorMessage = null) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.addCategory(uiState.value.type, name)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(uiState.value.type, name)
        }
    }

    fun save() {
        val state = uiState.value
        val amountInCents = MoneyFormatter.parseToCents(state.amountInput)
        if (amountInCents == null || amountInCents <= 0L) {
            formState.update { it.copy(errorMessage = "请输入有效金额") }
            return
        }

        val refundedAmount = if (state.type == TransactionType.EXPENSE) {
            if (state.refundInput.isBlank()) {
                0L
            } else {
                MoneyFormatter.parseToCents(state.refundInput)
                    ?: run {
                        formState.update { it.copy(errorMessage = "请输入有效退款金额") }
                        return
                    }
            }
        } else {
            0L
        }

        if (refundedAmount < 0L || refundedAmount > amountInCents) {
            formState.update { it.copy(errorMessage = "退款金额不能大于支出金额") }
            return
        }

        if (state.category.isBlank()) {
            formState.update { it.copy(errorMessage = "请先新增或选择分类") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            transactionRepository.saveTransaction(
                id = if (state.isExisting) state.id else 0L,
                type = state.type,
                amount = amountInCents,
                refundedAmount = refundedAmount,
                category = state.category,
                note = state.note.trim(),
                occurredAt = state.occurredAt,
                createdAt = if (state.isExisting) state.createdAt else now,
                updatedAt = now,
            )
            _events.emit(TransactionEditorEvent.Saved)
        }
    }

    fun delete() {
        val id = uiState.value.id
        if (id == 0L) {
            return
        }

        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            _events.emit(TransactionEditorEvent.Deleted)
        }
    }
}
