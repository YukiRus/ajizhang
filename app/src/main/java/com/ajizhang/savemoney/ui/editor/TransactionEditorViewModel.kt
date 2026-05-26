package com.ajizhang.savemoney.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.remote.LlmExpenseRecognizer
import com.ajizhang.savemoney.data.model.SubBudget
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.data.repository.BudgetRepository
import com.ajizhang.savemoney.data.repository.CategoryRepository
import com.ajizhang.savemoney.data.repository.TransactionRepository
import com.ajizhang.savemoney.data.voice.WavAudioRecorder
import com.ajizhang.savemoney.ui.navigation.Routes
import com.ajizhang.savemoney.util.DateFormatter
import com.ajizhang.savemoney.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val audioRecorder: WavAudioRecorder,
    private val llmExpenseRecognizer: LlmExpenseRecognizer,
) : ViewModel() {
    private val transactionId: Long =
        savedStateHandle.get<Long>(Routes.ARG_TRANSACTION_ID) ?: Routes.NEW_TRANSACTION_ID

    private val formState = MutableStateFlow(TransactionEditorUiState())

    private val currentMonthKey = DateFormatter.currentMonthKey()
    private val subBudgetsFlow: Flow<List<SubBudget>> =
        budgetRepository.observeBudgetByMonth(currentMonthKey)
            .flatMapLatest { budget ->
                if (budget != null) {
                    budgetRepository.observeSubBudgets(budget.id)
                } else {
                    flowOf(emptyList())
                }
            }

    val uiState: StateFlow<TransactionEditorUiState> = combine(
        formState,
        categoryRepository.observeAllCategories(),
        subBudgetsFlow,
    ) { state, categoryMap, subBudgets ->
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
            subBudgetOptions = subBudgets,
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
                            subBudgetId = transaction.subBudgetId,
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
                isVoiceRecording = false,
                isVoiceRecognizing = false,
                voiceStatusMessage = null,
                voiceErrorMessage = null,
                llmRawResponse = "",
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

    fun onSubBudgetChange(subBudgetId: Long?) {
        formState.update { it.copy(subBudgetId = subBudgetId, errorMessage = null) }
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

    fun startVoiceRecording() {
        val state = uiState.value
        if (state.isExisting || state.type != TransactionType.EXPENSE || state.isVoiceRecognizing) {
            return
        }

        viewModelScope.launch {
            audioRecorder.start()
                .onSuccess {
                    formState.update {
                        it.copy(
                            isVoiceRecording = true,
                            voiceStatusMessage = "录音中，松手后识别",
                            voiceErrorMessage = null,
                            llmRawResponse = "",
                        )
                    }
                }.onFailure { throwable ->
                    formState.update {
                        it.copy(
                            isVoiceRecording = false,
                            voiceStatusMessage = null,
                            voiceErrorMessage = throwable.message ?: "无法开始录音",
                        )
                    }
                }
        }
    }

    fun stopVoiceRecordingAndRecognize() {
        if (!uiState.value.isVoiceRecording) {
            return
        }

        viewModelScope.launch {
            val clip = audioRecorder.stop()
            if (clip == null) {
                formState.update {
                    it.copy(
                        isVoiceRecording = false,
                        voiceStatusMessage = null,
                        voiceErrorMessage = "录音失败，请重试",
                    )
                }
                return@launch
            }

            if (clip.durationMs < 1_000L) {
                formState.update {
                    it.copy(
                        isVoiceRecording = false,
                        voiceStatusMessage = null,
                        voiceErrorMessage = "录音时间太短，请至少说 1 秒",
                    )
                }
                return@launch
            }

            val categories = uiState.value.categories
            val subBudgets = uiState.value.subBudgetOptions
            val subBudgetNames = subBudgets.map { it.name }
            formState.update {
                it.copy(
                    isVoiceRecording = false,
                    isVoiceRecognizing = true,
                    voiceStatusMessage = "正在识别支出内容...",
                    voiceErrorMessage = null,
                    llmRawResponse = "",
                )
            }

            llmExpenseRecognizer.recognizeExpense(
                wavBytes = clip.wavBytes,
                categories = categories,
                subBudgetNames = subBudgetNames,
                today = LocalDate.now(),
                now = LocalDateTime.now(),
            ).onSuccess { result ->
                val resolvedCategory = resolveRecognizedCategory(
                    recognizedCategory = result.category,
                    categories = categories,
                    fallbackCategory = uiState.value.category,
                )
                formState.update { current ->
                    val parsedAmount = MoneyFormatter.parseToCents(result.amountText)
                    val parsedOccurredAt = parseRecognizedOccurredAt(result.dateText, result.timeText)
                    val resolvedSubBudgetId = subBudgets
                        .firstOrNull { it.name == result.budgetSubName.trim() }
                        ?.id
                    current.copy(
                        amountInput = parsedAmount?.let(MoneyFormatter::toInputValue) ?: current.amountInput,
                        category = resolvedCategory,
                        note = result.note.ifBlank { current.note },
                        occurredAt = parsedOccurredAt ?: current.occurredAt,
                        subBudgetId = resolvedSubBudgetId ?: current.subBudgetId,
                        isVoiceRecognizing = false,
                        voiceStatusMessage = "已根据语音填写，请确认后保存",
                        voiceErrorMessage = if (parsedAmount == null) "未识别出有效金额，请手动补充" else null,
                        llmRawResponse = result.rawResponse,
                    )
                }
            }.onFailure { throwable ->
                formState.update {
                    it.copy(
                        isVoiceRecognizing = false,
                        voiceStatusMessage = null,
                        voiceErrorMessage = throwable.message ?: "语音识别失败",
                        llmRawResponse = "",
                    )
                }
            }
        }
    }

    fun cancelVoiceRecording() {
        viewModelScope.launch {
            audioRecorder.cancel()
            formState.update {
                it.copy(
                    isVoiceRecording = false,
                    voiceStatusMessage = null,
                    llmRawResponse = "",
                )
            }
        }
    }

    private fun resolveRecognizedCategory(
        recognizedCategory: String,
        categories: List<String>,
        fallbackCategory: String,
    ): String {
        val trimmed = recognizedCategory.trim()
        if (trimmed.isBlank()) {
            return fallbackCategory
        }

        categories.firstOrNull { it == trimmed }?.let { return it }
        val normalized = trimmed.normalizeCategory()
        categories.firstOrNull { it.normalizeCategory() == normalized }?.let { return it }
        categories.firstOrNull {
            val candidate = it.normalizeCategory()
            normalized.contains(candidate) || candidate.contains(normalized)
        }?.let { return it }

        return fallbackCategory
    }

    private fun String.normalizeCategory(): String =
        trim()
            .replace("支出", "")
            .replace("消费", "")
            .replace("分类", "")
            .replace("类", "")
            .replace("：", "")
            .replace(":", "")
            .replace(" ", "")

    private fun parseRecognizedOccurredAt(
        dateText: String,
        timeText: String,
    ): Long? {
        val localDate = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull() ?: return null
        val localTime = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull() ?: LocalTime.MIDNIGHT
        return DateFormatter.localDateTimeToEpochMillis(LocalDateTime.of(localDate, localTime))
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
                subBudgetId = if (state.type == TransactionType.EXPENSE) state.subBudgetId else null,
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
