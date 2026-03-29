package com.ajizhang.savemoney.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.util.DateFormatter
import com.ajizhang.savemoney.util.MoneyFormatter

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                onAddTransaction = onAddTransaction,
                onEditTransaction = onEditTransaction,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showGoalDialog) {
        GoalEditorDialog(
            initialName = uiState.goalName.ifBlank { "我的攒钱目标" },
            initialTargetAmount = uiState.targetAmount,
            onDismiss = { showGoalDialog = false },
            onConfirm = { name, targetAmount ->
                viewModel.saveGoal(name, targetAmount)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (uiState.hasGoal) uiState.goalName else "还没有设置攒钱目标",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (uiState.hasGoal) {
                            "目标金额 ${MoneyFormatter.format(uiState.targetAmount ?: 0L)}"
                        } else {
                            "先设定目标，再开始追踪进度"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            accentColor = Color(0xFF0D6E6E),
            actionLabel = "编辑",
            onAction = onEditInvestment,
        )
        BalanceCard(
            title = "存款",
            value = MoneyFormatter.format(depositAmount),
            hint = "收入 - 消费自动计算",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            accentColor = Color(0xFF1E5AA8),
        )
    }
}

@Composable
private fun TransactionSection(
    transactions: List<TransactionRecord>,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGroupingName by rememberSaveable { mutableStateOf(TransactionGrouping.MONTH.name) }
    var anchorDateEpoch by rememberSaveable { mutableStateOf(DateFormatter.todayEpochMillis()) }
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
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "收支明细",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = TransactionGroupingHelper.periodLabel(selectedGrouping, anchorDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onAddTransaction) {
                    Text("新增")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransactionGrouping.entries.forEach { grouping ->
                    FilterChip(
                        selected = grouping == selectedGrouping,
                        onClick = {
                            selectedGroupingName = grouping.name
                            anchorDateEpoch = DateFormatter.todayEpochMillis()
                        },
                        label = {
                            Text(
                                when (grouping) {
                                    TransactionGrouping.MONTH -> "月"
                                    TransactionGrouping.QUARTER -> "季"
                                    TransactionGrouping.YEAR -> "年"
                                },
                            )
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        anchorDateEpoch = DateFormatter.localDateToEpochMillis(
                            TransactionGroupingHelper.move(selectedGrouping, anchorDate, -1),
                        )
                    },
                ) {
                    Text("上一组")
                }
                Text(
                    text = TransactionGroupingHelper.periodLabel(selectedGrouping, anchorDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = {
                        anchorDateEpoch = DateFormatter.localDateToEpochMillis(
                            TransactionGroupingHelper.move(selectedGrouping, anchorDate, 1),
                        )
                    },
                ) {
                    Text("下一组")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            )
                        }
                    }
                }
            }
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
            style = MaterialTheme.typography.headlineSmall,
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
    accentColor: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 投资和存款的信息密度不同，这里用统一结构和最小高度来保持视觉对齐。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.08f),
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionRecord,
    onClick: () -> Unit,
) {
    val amountColor =
        if (transaction.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFB3261E)
    val signedAmount =
        if (transaction.type == TransactionType.INCOME) {
            "+${MoneyFormatter.format(transaction.amount)}"
        } else {
            "-${MoneyFormatter.format(transaction.amount)}"
        }
    val hasRefund = transaction.type == TransactionType.EXPENSE && transaction.refundedAmount > 0L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = DateFormatter.format(transaction.occurredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = signedAmount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(if (transaction.type == TransactionType.INCOME) "收入" else "消费") },
                )
                if (hasRefund) {
                    AssistChip(
                        onClick = { },
                        label = { Text("已退款 ${MoneyFormatter.format(transaction.refundedAmount)}") },
                    )
                }
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalEditorDialog(
    initialName: String,
    initialTargetAmount: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var targetInput by remember { mutableStateOf(initialTargetAmount?.let(MoneyFormatter::toInputValue).orEmpty()) }
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
                        onConfirm(name.trim(), parsed)
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
