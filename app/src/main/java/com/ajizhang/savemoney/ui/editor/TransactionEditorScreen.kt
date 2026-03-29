package com.ajizhang.savemoney.ui.editor

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.DateFormatter
import java.time.LocalDate
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorScreen(
    viewModel: TransactionEditorViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryManager by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                TransactionEditorEvent.Deleted,
                TransactionEditorEvent.Saved -> onBack()
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isExisting) "编辑记录" else "新增记录")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "保存记录",
                        )
                    }
                    if (uiState.isExisting) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "删除记录",
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("加载中...")
            }
        } else {
            EditorContent(
                uiState = uiState,
                onTypeChange = viewModel::onTypeChange,
                onAmountChange = viewModel::onAmountChange,
                onRefundChange = viewModel::onRefundChange,
                onCategoryChange = viewModel::onCategoryChange,
                onManageCategories = { showCategoryManager = true },
                onNoteChange = viewModel::onNoteChange,
                onDateChange = viewModel::onDateChange,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("删除后，这笔收支会从存款统计中移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            type = uiState.type,
            categories = uiState.categories,
            selectedCategory = uiState.category,
            onDismiss = { showCategoryManager = false },
            onAddCategory = viewModel::addCategory,
            onDeleteCategory = viewModel::deleteCategory,
            onSelectCategory = viewModel::onCategoryChange,
        )
    }
}

@Composable
private fun EditorContent(
    uiState: TransactionEditorUiState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onRefundChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onManageCategories: () -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "记录一笔新的现金流",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        TypeSelector(
            selectedType = uiState.type,
            onTypeChange = onTypeChange,
        )
        OutlinedTextField(
            value = uiState.amountInput,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("金额") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        if (uiState.type == TransactionType.EXPENSE) {
            OutlinedTextField(
                value = uiState.refundInput,
                onValueChange = onRefundChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("退款金额") },
                supportingText = {
                    Text("可选。填写后该支出会标记为已退款，并按净支出计入统计。")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        CategorySelector(
            selectedCategory = uiState.category,
            categories = uiState.categories,
            onCategoryChange = onCategoryChange,
            onManageCategories = onManageCategories,
        )
        DateSelector(
            occurredAt = uiState.occurredAt,
            onDateChange = onDateChange,
        )
        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注") },
            minLines = 3,
            maxLines = 4,
        )
        uiState.errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TypeSelector(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "类型",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = selectedType == TransactionType.INCOME,
                onClick = { onTypeChange(TransactionType.INCOME) },
                label = { Text("收入") },
            )
            FilterChip(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = { onTypeChange(TransactionType.EXPENSE) },
                label = { Text("消费") },
            )
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: String,
    categories: List<String>,
    onCategoryChange: (String) -> Unit,
    onManageCategories: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "分类",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectedCategory.isBlank()) "暂无分类，请先新增" else selectedCategory,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedCategory.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            TextButton(onClick = onManageCategories) {
                Text("管理分类")
            }
        }
        Box {
            FlatSelectorField(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = categories.isNotEmpty()) { expanded = true },
                value = if (selectedCategory.isBlank()) "请先新增分类" else selectedCategory,
                trailingText = if (categories.isEmpty()) "无可选分类" else "选择",
                placeholder = selectedCategory.isBlank(),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            expanded = false
                            onCategoryChange(category)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryManagerDialog(
    type: TransactionType,
    categories: List<String>,
    selectedCategory: String,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (type == TransactionType.EXPENSE) "管理支出分类" else "管理收入分类")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("新增分类") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val normalized = input.trim()
                                when {
                                    normalized.isBlank() -> error = "请输入分类名"
                                    normalized in categories -> error = "分类已存在"
                                    else -> {
                                        onAddCategory(normalized)
                                        input = ""
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "新增分类",
                            )
                        }
                    },
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (categories.isEmpty()) {
                    Text(
                        text = "当前还没有分类，先新增一个再保存记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            CategoryChip(
                                name = category,
                                isSelected = category == selectedCategory,
                                onSelect = { onSelectCategory(category) },
                                onDelete = { onDeleteCategory(category) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
    )
}

@Composable
private fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "删除分类",
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun DateSelector(
    occurredAt: Long,
    onDateChange: (Long) -> Unit,
) {
    val context = LocalContext.current
    val selectedDate = DateFormatter.epochMillisToLocalDate(occurredAt)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "日期",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlatSelectorField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onDateChange(
                                DateFormatter.localDateToEpochMillis(
                                    LocalDate.of(year, month + 1, dayOfMonth),
                                ),
                            )
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth,
                    ).show()
                },
            value = DateFormatter.format(occurredAt),
            trailingText = "更改",
        )
    }
}

@Composable
private fun FlatSelectorField(
    value: String,
    trailingText: String,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE1D6C7)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (placeholder) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
