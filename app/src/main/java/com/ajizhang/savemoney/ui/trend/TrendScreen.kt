package com.ajizhang.savemoney.ui.trend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajizhang.savemoney.data.model.TransactionRecord
import com.ajizhang.savemoney.data.model.TransactionType
import com.ajizhang.savemoney.domain.TrendPoint
import com.ajizhang.savemoney.domain.TrendRange
import com.ajizhang.savemoney.ui.home.TransactionSectionGroup
import com.ajizhang.savemoney.util.DateFormatter
import com.ajizhang.savemoney.util.MoneyFormatter
import kotlin.math.max

@Composable
fun TrendScreen(
    onBack: () -> Unit,
    viewModel: TrendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F3E7), Color(0xFFFDFBF6)),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrendHeader(
                title = uiState.title,
                selectedRange = uiState.selectedRange,
                selectedSeries = uiState.selectedSeries,
                excludedExpenseCategories = uiState.excludedExpenseCategories,
                availableExpenseCategories = uiState.availableExpenseCategories,
                onBack = onBack,
                onSelectRange = viewModel::selectRange,
                onToggleSeries = viewModel::toggleSeries,
                onToggleExpenseCategory = viewModel::toggleExpenseCategory,
                onClearExpenseCategoryFilters = viewModel::clearExpenseCategoryFilters,
                onMovePrevious = { viewModel.move(-1) },
                onMoveNext = { viewModel.move(1) },
            )
            TrendSummarySection(
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
                totalNet = uiState.totalNet,
            )
            TrendChartCard(
                points = uiState.points,
                selectedSeries = uiState.selectedSeries,
                hasData = uiState.hasData,
            )
            TrendDetailSection(sections = uiState.detailSections)
        }
    }
}

@Composable
private fun TrendHeader(
    title: String,
    selectedRange: TrendRange,
    selectedSeries: Set<TrendSeries>,
    excludedExpenseCategories: Set<String>,
    availableExpenseCategories: List<String>,
    onBack: () -> Unit,
    onSelectRange: (TrendRange) -> Unit,
    onToggleSeries: (TrendSeries) -> Unit,
    onToggleExpenseCategory: (String) -> Unit,
    onClearExpenseCategoryFilters: () -> Unit,
    onMovePrevious: () -> Unit,
    onMoveNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "收支趋势",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onMovePrevious) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "上一组",
                        )
                    }
                    IconButton(onClick = onMoveNext) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "下一组",
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendRange.entries.forEach { range ->
                    TrendRangeChip(
                        text = when (range) {
                            TrendRange.MONTH -> "月"
                            TrendRange.QUARTER -> "季"
                            TrendRange.YEAR -> "年"
                        },
                        selected = selectedRange == range,
                        onClick = { onSelectRange(range) },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendSeries.entries.forEach { series ->
                    TrendSeriesChip(
                        series = series,
                        selected = series in selectedSeries,
                        onClick = { onToggleSeries(series) },
                    )
                }
            }
            ExpenseCategoryFilter(
                availableCategories = availableExpenseCategories,
                excludedCategories = excludedExpenseCategories,
                onToggleCategory = onToggleExpenseCategory,
                onClear = onClearExpenseCategoryFilters,
            )
        }
    }
}

@Composable
private fun TrendSummarySection(
    totalIncome: Long,
    totalExpense: Long,
    totalNet: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryMetricCard(
            label = "收入",
            value = MoneyFormatter.format(totalIncome),
            color = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f),
        )
        SummaryMetricCard(
            label = "支出",
            value = MoneyFormatter.format(totalExpense),
            color = Color(0xFFB3261E),
            modifier = Modifier.weight(1f),
        )
        SummaryMetricCard(
            label = "净结余",
            value = MoneyFormatter.format(totalNet),
            color = Color(0xFFCC5A17),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TrendChartCard(
    points: List<TrendPoint>,
    selectedSeries: Set<TrendSeries>,
    hasData: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "趋势折线",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ChartLegend(selectedSeries = selectedSeries)
            if (hasData) {
                TrendLineChart(
                    points = points,
                    selectedSeries = selectedSeries,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "当前周期还没有记录",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "切换到其他月份、季度或年份查看趋势。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(
    selectedSeries: Set<TrendSeries>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        selectedSeries.forEach { series ->
            LegendItem(
                label = series.label,
                color = series.color,
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp)
                .background(color = color, shape = RoundedCornerShape(999.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TrendLineChart(
    points: List<TrendPoint>,
    selectedSeries: Set<TrendSeries>,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val chartHeight = 240.dp
    val yLabelWidth = 64.dp
    val minPointWidth = 36.dp
    val values = points.flatMap { point -> selectedSeries.map { series -> point.valueOf(series) } }
    val rawMin = values.minOrNull() ?: 0L
    val rawMax = values.maxOrNull() ?: 0L
    val minValue = minOf(0L, rawMin)
    val maxValue = maxOf(0L, rawMax)
    val segments = 4
    val labelStep = max(1, points.size / 6)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val availableChartWidth = maxWidth - yLabelWidth - 8.dp
        val contentWidth = max(
            with(density) { availableChartWidth.roundToPx() },
            with(density) { (minPointWidth * points.size).roundToPx() },
        )
        val chartWidth = with(density) { contentWidth.toDp() }

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(bottom = 4.dp),
        ) {
            YAxisLabels(
                minValue = minValue,
                maxValue = maxValue,
                segments = segments,
                chartHeight = chartHeight,
                modifier = Modifier.width(yLabelWidth),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.width(chartWidth),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                ) {
                    val chartMin = minValue.toFloat()
                    val chartMax = maxValue.toFloat()
                    val valueRange = (chartMax - chartMin).takeIf { it != 0f } ?: 1f

                    repeat(segments + 1) { index ->
                        val y = size.height * (index / segments.toFloat())
                        drawLine(
                            color = Color(0xFFE6DBCB),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
                    fun pointOffset(index: Int, value: Long): Offset {
                        val progress = (value.toFloat() - chartMin) / valueRange
                        return Offset(
                            x = if (points.size > 1) stepX * index else size.width / 2f,
                            y = size.height - (progress * size.height),
                        )
                    }

                    selectedSeries.forEach { series ->
                        drawSeries(
                            points = points.mapIndexed { index, point ->
                                pointOffset(index, point.valueOf(series))
                            },
                            color = series.color,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    points.forEachIndexed { index, point ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            val shouldShowLabel =
                                index == 0 || index == points.lastIndex || index % labelStep == 0
                            Text(
                                text = if (shouldShowLabel) point.label else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSeries(
    points: List<Offset>,
    color: Color,
) {
    if (points.isEmpty()) {
        return
    }

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx()),
    )
    points.forEach { point ->
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = point,
        )
    }
}

@Composable
private fun YAxisLabels(
    minValue: Long,
    maxValue: Long,
    segments: Int,
    chartHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.height(chartHeight),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        (0..segments).forEach { index ->
            val value = maxValue - ((maxValue - minValue) * index / segments)
            Text(
                text = MoneyFormatter.format(value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendDetailSection(
    sections: List<TransactionSectionGroup>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "本周期明细",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "当前周期下没有记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                sections.forEach { section ->
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    section.transactions.forEach { transaction ->
                        TrendTransactionItem(transaction = transaction)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendTransactionItem(
    transaction: TransactionRecord,
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
        modifier = Modifier.fillMaxWidth(),
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
                    TrendTransactionTag(if (transaction.type == TransactionType.INCOME) "收入" else "消费")
                    if (hasRefund) {
                        TrendTransactionTag("退款 ${MoneyFormatter.format(transaction.refundedAmount)}")
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
                    append(DateFormatter.formatWithOptionalTime(transaction.occurredAt))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendTransactionTag(
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
private fun TrendRangeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFFE6D7BF) else Color(0xFFF7F1E7),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFFD3B07A) else Color(0xFFE6DBCB),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseCategoryFilter(
    availableCategories: List<String>,
    excludedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (availableCategories.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "排除支出类别",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点亮的类别不会计入支出、净结余和明细",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (excludedCategories.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("清除")
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableCategories.forEach { category ->
                ExpenseCategoryChip(
                    text = category,
                    excluded = category in excludedCategories,
                    onClick = { onToggleCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun ExpenseCategoryChip(
    text: String,
    excluded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (excluded) Color(0xFFFBE8E6) else Color(0xFFF7F1E7),
        border = BorderStroke(
            width = 1.dp,
            color = if (excluded) Color(0xFFE0A09A) else Color(0xFFE6DBCB),
        ),
    ) {
        Text(
            text = if (excluded) "已排除 $text" else text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (excluded) Color(0xFFB3261E) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (excluded) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun TrendSeriesChip(
    series: TrendSeries,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) series.color.copy(alpha = 0.14f) else Color(0xFFF7F1E7),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) series.color.copy(alpha = 0.55f) else Color(0xFFE6DBCB),
        ),
    ) {
        Text(
            text = series.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) series.color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private val TrendSeries.label: String
    get() = when (this) {
        TrendSeries.INCOME -> "收入"
        TrendSeries.EXPENSE -> "支出"
        TrendSeries.NET -> "净结余"
    }

private val TrendSeries.color: Color
    get() = when (this) {
        TrendSeries.INCOME -> Color(0xFF2E7D32)
        TrendSeries.EXPENSE -> Color(0xFFB3261E)
        TrendSeries.NET -> Color(0xFFCC5A17)
    }

private fun TrendPoint.valueOf(series: TrendSeries): Long =
    when (series) {
        TrendSeries.INCOME -> income
        TrendSeries.EXPENSE -> expense
        TrendSeries.NET -> net
    }
