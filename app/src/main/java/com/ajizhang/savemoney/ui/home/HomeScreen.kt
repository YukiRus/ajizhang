package com.ajizhang.savemoney.ui.home

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.DateFormatter
import com.ajizhang.savemoney.util.MoneyFormatter
import java.time.LocalDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showInvestmentDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "新增记录",
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F3E7), Color(0xFFFDFBF6)),
                    ),
                )
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GoalSection(
                uiState = uiState,
                onEditGoal = { showGoalDialog = true },
            )
            BalancesSection(
                investmentAmount = uiState.investmentAmount,
                depositAmount = uiState.depositAmount,
                onEditInvestment = { showInvestmentDialog = true },
            )
            TransactionSection(
                transactions = uiState.transactions,
                onEditTransaction = onEditTransaction,
                onDeleteTransaction = viewModel::deleteTransaction,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showGoalDialog) {
        GoalEditorDialog(
            initialName = uiState.goalName.ifBlank { "我的攒钱目标" },
            initialTargetAmount = uiState.targetAmount,
            initialExpectedDate = uiState.expectedDate,
            onDismiss = { showGoalDialog = false },
            onConfirm = { name, targetAmount, expectedDate ->
                viewModel.saveGoal(name, targetAmount, expectedDate)
                showGoalDialog = false
            },
        )
    }

    if (showInvestmentDialog) {
        InvestmentEditorDialog(
            initialAmount = uiState.investmentAmount,
            onDismiss = { showInvestmentDialog = false },
            onConfirm = { amount ->
                viewModel.saveInvestment(amount)
                showInvestmentDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSection(
    uiState: HomeUiState,
    onEditGoal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (uiState.hasGoal) uiState.goalName else "还没有设置攒钱目标",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        uiState.expectedDate?.let { expectedDate ->
                            GoalDateBadge(expectedDate = expectedDate)
                        }
                    }
                    Text(
                        text = if (uiState.hasGoal) {
                            "目标金额 ${MoneyFormatter.format(uiState.targetAmount ?: 0L)}"
                        } else {
                            "先设定目标，再开始追踪进度"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GoalRecommendationText(uiState = uiState)
                }
                TextButton(onClick = onEditGoal) {
                    Text(if (uiState.hasGoal) "编辑目标" else "设置目标")
                }
            }

            SummaryLine(
                label = "已攒金额",
                value = MoneyFormatter.format(uiState.savedAmount),
                accentColor = Color(0xFF2E7D32),
            )
            SummaryLine(
                label = "还差金额",
                value = uiState.remainingAmount?.let(MoneyFormatter::format) ?: "未设置",
                accentColor = Color(0xFFCC5A17),
            )
        }
    }
}

@Composable
private fun BalancesSection(
    investmentAmount: Long,
    depositAmount: Long,
    onEditInvestment: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BalanceCard(
            title = "投资",
            value = MoneyFormatter.format(investmentAmount),
            hint = "直接编辑当前余额",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            statusLabel = "可编辑",
            onClick = onEditInvestment,
        )
        BalanceCard(
            title = "存款",
            value = MoneyFormatter.format(depositAmount),
            hint = "收入 - 消费自动计算",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            statusLabel = "自动统计",
        )
    }
}

@Composable
private fun TransactionSection(
    transactions: List<TransactionRecord>,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGroupingName by rememberSaveable { mutableStateOf(TransactionGrouping.MONTH.name) }
    var anchorDateEpoch by rememberSaveable { mutableStateOf(DateFormatter.todayEpochMillis()) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    val selectedGrouping = TransactionGrouping.valueOf(selectedGroupingName)
    val anchorDate = DateFormatter.epochMillisToLocalDate(anchorDateEpoch)
    val sections = remember(transactions, selectedGrouping, anchorDateEpoch) {
        TransactionGroupingHelper.buildSections(
            transactions = transactions,
            grouping = selectedGrouping,
            anchorDate = anchorDate,
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactTransactionHeader(
                selectedGrouping = selectedGrouping,
                anchorDateLabel = TransactionGroupingHelper.periodLabel(selectedGrouping, anchorDate),
                onSelectGrouping = { grouping ->
                    selectedGroupingName = grouping.name
                    anchorDateEpoch = DateFormatter.todayEpochMillis()
                },
                onMovePrevious = {
                    anchorDateEpoch = DateFormatter.localDateToEpochMillis(
                        TransactionGroupingHelper.move(selectedGrouping, anchorDate, -1),
                    )
                },
                onMoveNext = {
                    anchorDateEpoch = DateFormatter.localDateToEpochMillis(
                        TransactionGroupingHelper.move(selectedGrouping, anchorDate, 1),
                    )
                },
            )
            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "当前分组下没有记录",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "可以切换到其他月份、季度或年份查看。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sections.forEach { section ->
                        item(key = section.title) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        items(
                            items = section.transactions,
                            key = { it.id },
                        ) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onEditTransaction(transaction.id) },
                                onDelete = { pendingDeleteTransaction = transaction },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeleteTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("删除记录") },
            text = {
                Text("删除后，这笔${if (transaction.type == TransactionType.INCOME) "收入" else "支出"}会从统计中移除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(transaction.id)
                        pendingDeleteTransaction = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun CompactTransactionHeader(
    selectedGrouping: TransactionGrouping,
    anchorDateLabel: String,
    onSelectGrouping: (TransactionGrouping) -> Unit,
    onMovePrevious: () -> Unit,
    onMoveNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransactionGrouping.entries.forEach { grouping ->
                GroupingOptionChip(
                    text = when (grouping) {
                        TransactionGrouping.MONTH -> "月"
                        TransactionGrouping.QUARTER -> "季"
                        TransactionGrouping.YEAR -> "年"
                    },
                    selected = grouping == selectedGrouping,
                    onClick = { onSelectGrouping(grouping) },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactNavigatorButton(
                text = "上一组",
                onClick = onMovePrevious,
            )
            Text(
                text = anchorDateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompactNavigatorButton(
                text = "下一组",
                onClick = onMoveNext,
            )
        }
    }
}

@Composable
private fun GroupingOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFFE6D7BF) else Color(0xFFF7F1E7),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFFD3B07A) else Color(0xFFE6DBCB),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CompactNavigatorButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun GoalDateBadge(
    expectedDate: Long,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF5EFE5),
    ) {
        Text(
            text = "预计 ${DateFormatter.format(expectedDate)}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalRecommendationText(
    uiState: HomeUiState,
) {
    when {
        uiState.recommendedMonthlyAmount != null -> {
            Text(
                text = "建议每月应攒 ${MoneyFormatter.format(uiState.recommendedMonthlyAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        uiState.expectedDate != null && uiState.remainingAmount == 0L -> {
            Text(
                text = "目标已达成",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.SemiBold,
            )
        }

        uiState.isExpectedDatePassed -> {
            Text(
                text = "预计时间已过，建议调整目标日期",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BalanceCard(
    title: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    statusLabel: String,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE3D7C6)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = statusLabel,
                    modifier = Modifier
                        .background(
                            color = Color(0xFFF5EFE5),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionItem(
    transaction: TransactionRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showActionMenu by rememberSaveable(transaction.id) { mutableStateOf(false) }
    val amountColor =
        if (transaction.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFB3261E)
    val signedAmount =
        if (transaction.type == TransactionType.INCOME) {
            "+${MoneyFormatter.format(transaction.amount)}"
        } else {
            "-${MoneyFormatter.format(transaction.amount)}"
        }
    val hasRefund = transaction.type == TransactionType.EXPENSE && transaction.refundedAmount > 0L

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showActionMenu = true },
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TransactionTag(if (transaction.type == TransactionType.INCOME) "收入" else "消费")
                        if (hasRefund) {
                            TransactionTag("退款 ${MoneyFormatter.format(transaction.refundedAmount)}")
                        }
                    }
                    Text(
                        text = signedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                }
                Text(
                    text = buildString {
                        if (transaction.note.isNotBlank()) {
                            append(transaction.note)
                            append(" · ")
                        }
                        append(DateFormatter.format(transaction.occurredAt))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DropdownMenu(
            expanded = showActionMenu,
            onDismissRequest = { showActionMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showActionMenu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun TransactionTag(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF5EFE5),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalEditorDialog(
    initialName: String,
    initialTargetAmount: Long?,
    initialExpectedDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long?) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var targetInput by remember { mutableStateOf(initialTargetAmount?.let(MoneyFormatter::toInputValue).orEmpty()) }
    var expectedDate by remember { mutableStateOf(initialExpectedDate) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置攒钱目标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("目标名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = {
                        targetInput = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("目标金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    text = "预计完成日期",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val selectedDate =
                                expectedDate?.let(DateFormatter::epochMillisToLocalDate) ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    expectedDate = DateFormatter.localDateToEpochMillis(
                                        LocalDate.of(year, month + 1, dayOfMonth),
                                    )
                                    error = null
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth,
                            ).show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = expectedDate?.let(DateFormatter::format) ?: "未设置",
                            color = if (expectedDate == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(
                            text = if (expectedDate == null) "选择日期" else "修改",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (expectedDate != null) {
                    TextButton(
                        onClick = {
                            expectedDate = null
                            error = null
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("清空日期")
                    }
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = MoneyFormatter.parseToCents(targetInput)
                    if (name.isBlank()) {
                        error = "请输入目标名称"
                    } else if (parsed == null || parsed <= 0L) {
                        error = "请输入有效目标金额"
                    } else {
                        onConfirm(name.trim(), parsed, expectedDate)
                    }
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun InvestmentEditorDialog(
    initialAmount: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var amountInput by remember { mutableStateOf(MoneyFormatter.toInputValue(initialAmount)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑投资余额") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("当前投资金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    text = "这里改的是投资当前余额，不会生成单独的收支记录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = MoneyFormatter.parseToCents(amountInput)
                    if (parsed == null || parsed < 0L) {
                        error = "请输入有效金额"
                    } else {
                        onConfirm(parsed)
                    }
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
