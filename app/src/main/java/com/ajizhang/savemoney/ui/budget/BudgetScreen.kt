package com.ajizhang.savemoney.ui.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajizhang.savemoney.data.model.SubBudget
import com.ajizhang.savemoney.util.MoneyFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSubDialog by rememberSaveable { mutableStateOf(false) }
    var showCopyDialog by rememberSaveable { mutableStateOf(false) }
    var showMainBudgetDialog by rememberSaveable { mutableStateOf(false) }

    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonthSelector(
            monthKey = uiState.monthKey,
            onPrevious = viewModel::goToPreviousMonth,
            onNext = viewModel::goToNextMonth,
            onMonthSelected = viewModel::setMonthKey,
        )

        MainBudgetCard(
            monthlyBudget = uiState.monthlyBudget,
            totalSpent = uiState.totalSpent,
            totalSubBudgetSum = uiState.totalSubBudgetSum,
            onEdit = { showMainBudgetDialog = true },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "子预算",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.monthlyBudget != null && uiState.subBudgets.isNotEmpty()) {
                    TextButton(onClick = { showCopyDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("复制", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                IconButton(onClick = { showAddSubDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "新增子预算",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (uiState.subBudgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无子预算\n点击 + 新增",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            uiState.subBudgets.forEach { subBudget ->
                SubBudgetCard(
                    subBudget = subBudget,
                    mainBudgetAmount = uiState.monthlyBudget?.totalAmount ?: 0,
                    onClick = { viewModel.openSubBudgetDetail(subBudget) },
                    onEdit = { id, name, amount ->
                        val error = viewModel.updateSubBudget(id, name, amount)
                        error
                    },
                    onDelete = { viewModel.deleteSubBudget(subBudget.id) },
                )
            }
        }
    }

    if (showMainBudgetDialog) {
        MainBudgetEditDialog(
            currentAmount = uiState.monthlyBudget?.totalAmount?.let(MoneyFormatter::toInputValue) ?: "",
            onDismiss = { showMainBudgetDialog = false },
            onSave = { input ->
                viewModel.saveMainBudget(input)
                showMainBudgetDialog = false
            },
        )
    }

    if (showAddSubDialog) {
        SubBudgetEditDialog(
            title = "新增子预算",
            initialName = "",
            initialAmount = "",
            remainingBudgetCents = (uiState.monthlyBudget?.totalAmount ?: 0) - uiState.totalSubBudgetSum,
            onDismiss = { showAddSubDialog = false },
            onSave = { name, amount ->
                val error = viewModel.addSubBudget(name, amount)
                if (error != null) {
                    return@SubBudgetEditDialog error
                }
                showAddSubDialog = false
                null
            },
        )
    }

    if (showCopyDialog) {
        CopyBudgetDialog(
            currentMonthKey = uiState.monthKey,
            getMonthKeys = viewModel::getMonthKeysForDropdown,
            onDismiss = { showCopyDialog = false },
            onCopy = { targetMonth ->
                viewModel.copyToMonth(targetMonth)
                showCopyDialog = false
            },
        )
    }

    detailState.subBudget?.let { subBudget ->
        SubBudgetDetailDialog(
            subBudget = subBudget,
            transactions = detailState.transactions,
            isLoading = detailState.isLoading,
            onDismiss = viewModel::closeSubBudgetDetail,
        )
    }
}

@Composable
private fun MonthSelector(
    monthKey: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMonthSelected: (String) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val displayText = remember(monthKey) {
        val parts = monthKey.split("-")
        "${parts[0]}年${parts[1]}月"
    }
    val parts = remember(monthKey) { monthKey.split("-") }
    val currentYear = parts[0].toInt()
    val currentMonth = parts[1].toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F1E7),
        border = BorderStroke(1.dp, Color(0xFFE1D6C7)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "上个月",
                )
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showPicker = true },
            )
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "下个月",
                )
            }
        }
    }

    if (showPicker) {
        MonthPickerDialog(
            initialYear = currentYear,
            initialMonth = currentMonth,
            onDismiss = { showPicker = false },
            onMonthSelected = { year, month ->
                onMonthSelected("%04d-%02d".format(year, month))
                showPicker = false
            },
        )
    }
}

@Composable
private fun MainBudgetCard(
    monthlyBudget: com.ajizhang.savemoney.data.model.MonthlyBudget?,
    totalSpent: Long,
    totalSubBudgetSum: Long,
    onEdit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFBF6),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "本月总预算",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onEdit) {
                    Text(if (monthlyBudget != null) "修改" else "设置")
                }
            }

            if (monthlyBudget != null) {
                val progress = if (monthlyBudget.totalAmount > 0) {
                    (totalSpent.toFloat() / monthlyBudget.totalAmount).coerceIn(0f, 1f)
                } else 0f

                Text(
                    text = MoneyFormatter.format(monthlyBudget.totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (progress > 1f) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFFE9DFCF),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "已使用 ${MoneyFormatter.format(totalSpent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "剩余 ${MoneyFormatter.format((monthlyBudget.totalAmount - totalSpent).coerceAtLeast(0))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalSpent > monthlyBudget.totalAmount) Color(0xFFB3261E)
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                if (totalSubBudgetSum > monthlyBudget.totalAmount) {
                    Text(
                        text = "子预算总计(${MoneyFormatter.format(totalSubBudgetSum)})超过主预算",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Text(
                    text = "尚未设置本月预算",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("设置预算")
                }
            }
        }
    }
}

@Composable
private fun SubBudgetCard(
    subBudget: SubBudget,
    mainBudgetAmount: Long,
    onClick: () -> Unit,
    onEdit: (id: Long, name: String, amount: String) -> String?,
    onDelete: () -> Unit,
) {
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val progress = if (subBudget.amount > 0) {
        (subBudget.spent.toFloat() / subBudget.amount).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFBF6),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subBudget.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showEditDialog = true }) {
                        Text("编辑", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFB3261E),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress > 1f) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE9DFCF),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "额度 ${subBudget.amountText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "已用 ${subBudget.spentText} / 剩余 ${subBudget.remainingText}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (subBudget.spent > subBudget.amount) Color(0xFFB3261E)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showEditDialog) {
        SubBudgetEditDialog(
            title = "编辑子预算",
            initialName = subBudget.name,
            initialAmount = MoneyFormatter.toInputValue(subBudget.amount),
            onDismiss = { showEditDialog = false },
            onSave = { name, amount ->
                val error = onEdit(subBudget.id, name, amount)
                if (error != null) {
                    return@SubBudgetEditDialog error
                }
                showEditDialog = false
                null
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除子预算") },
            text = { Text("删除「${subBudget.name}」后，已关联的支出将取消关联。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("删除", color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun MainBudgetEditDialog(
    currentAmount: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(currentAmount) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置月度总预算") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("预算金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = MoneyFormatter.parseToCents(input)
                if (amount == null || amount <= 0L) {
                    error = "请输入有效金额"
                } else {
                    onSave(input)
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
fun SubBudgetEditDialog(
    title: String,
    initialName: String,
    initialAmount: String,
    remainingBudgetCents: Long? = null,
    onDismiss: () -> Unit,
    onSave: (String, String) -> String?,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("子预算名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("预算额度") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (remainingBudgetCents != null && remainingBudgetCents > 0) {
                    TextButton(
                        onClick = {
                            amount = MoneyFormatter.toInputValue(remainingBudgetCents)
                            error = null
                        },
                    ) {
                        Text("使用剩余预算 ${MoneyFormatter.format(remainingBudgetCents)}")
                    }
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> error = "请输入名称"
                    MoneyFormatter.parseToCents(amount) == null || MoneyFormatter.parseToCents(amount)!! <= 0L ->
                        error = "请输入有效金额"
                    else -> {
                        val validationError = onSave(name, amount)
                        if (validationError != null) {
                            error = validationError
                        }
                    }
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun CopyBudgetDialog(
    currentMonthKey: String,
    getMonthKeys: () -> List<String>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val parts = remember(currentMonthKey) { currentMonthKey.split("-") }
    val currentYear = parts[0].toInt()
    val currentMonth = parts[1].toInt()
    var year by remember { mutableStateOf(currentYear) }
    var selectedMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    val monthLabels = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月",
    )

    val yearString = "${year}年"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { year--; selectedMonth = null }, modifier = Modifier.size(40.dp)) {
                    Text("◀", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = yearString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = { year++; selectedMonth = null }, modifier = Modifier.size(40.dp)) {
                    Text("▶", fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "将当前月份的子预算结构和额度复制到目标月份。已花费金额不会复制。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (col in 0..3) {
                                val monthIndex = row * 4 + col
                                val month = monthIndex + 1
                                val label = monthLabels[monthIndex]
                                val isCurrent = year == currentYear && month == currentMonth
                                val isSelected = selectedMonth == month
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = !isCurrent) {
                                            selectedMonth = month
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.surfaceVariant
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else -> Color(0xFFF7F1E7)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> Color.Transparent
                                        },
                                    ),
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        } else {
                                            Color.Unspecified
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedMonth?.let {
                        onCopy("%04d-%02d".format(year, it))
                    }
                },
                enabled = selectedMonth != null,
            ) {
                Text("复制")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun MonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit,
) {
    var year by remember { mutableStateOf(initialYear) }
    val months = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { year-- }, modifier = Modifier.size(36.dp)) {
                    Text("◀", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${year}年",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { year++ }, modifier = Modifier.size(36.dp)) {
                    Text("▶", fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (col in 0..3) {
                            val monthIndex = row * 4 + col
                            val month = months[monthIndex]
                            val isSelected = monthIndex + 1 == initialMonth && year == initialYear
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onMonthSelected(year, monthIndex + 1) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    Color(0xFFF7F1E7)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                            ) {
                                Text(
                                    text = month,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SubBudgetDetailDialog(
    subBudget: SubBudget,
    transactions: List<com.ajizhang.savemoney.data.model.TransactionRecord>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${subBudget.name} 的支出明细")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "额度 ${subBudget.amountText}  /  已用 ${subBudget.spentText}  /  剩余 ${subBudget.remainingText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("加载中...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (transactions.isEmpty()) {
                    Text(
                        text = "暂无关联支出",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    transactions.forEach { transaction ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF7F1E7),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = transaction.category,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = com.ajizhang.savemoney.util.MoneyFormatter.format(
                                            (transaction.amount - transaction.refundedAmount).coerceAtLeast(0),
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (transaction.note.isNotBlank()) {
                                    Text(
                                        text = transaction.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
