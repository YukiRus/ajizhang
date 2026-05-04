package com.ajizhang.savemoney.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ajizhang.savemoney.data.model.RecognizedExpenseItem
import com.ajizhang.savemoney.util.MoneyFormatter
import java.util.UUID

private data class EditableItem(
    val uid: String,
    val amountText: String,
    val category: String,
    val note: String,
    val dateText: String,
    val timeText: String,
)

@Composable
fun ImageExpenseDialog(
    items: List<RecognizedExpenseItem>,
    categories: List<String>,
    rawLlmResponse: String,
    onDismiss: () -> Unit,
    onSave: (List<RecognizedExpenseItem>) -> Unit,
) {
    val editableItems = remember(items) {
        mutableStateListOf<EditableItem>().apply {
            addAll(
                items.map {
                    EditableItem(
                        uid = UUID.randomUUID().toString(),
                        amountText = it.amountText,
                        category = it.category,
                        note = it.note,
                        dateText = it.dateText,
                        timeText = it.timeText,
                    )
                },
            )
        }
    }
    var showRawResponse by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "图片识别结果",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                        )
                    }
                }

                if (editableItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "未识别到任何支出项目",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        editableItems.forEachIndexed { index, item ->
                            ExpenseEditCard(
                                item = item,
                                categories = categories,
                                onUpdate = { updated ->
                                    editableItems[index] = updated
                                },
                                onDelete = {
                                    editableItems.removeAt(index)
                                },
                            )
                        }
                    }
                }

                if (rawLlmResponse.isNotBlank()) {
                    TextButton(
                        onClick = { showRawResponse = !showRawResponse },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Text(if (showRawResponse) "收起原始回复" else "查看 LLM 原始回复")
                    }
                    if (showRawResponse) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF7F1E7),
                            border = BorderStroke(1.dp, Color(0xFFE1D6C7)),
                        ) {
                            Text(
                                text = rawLlmResponse,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            val result = editableItems.map {
                                RecognizedExpenseItem(
                                    amountText = it.amountText,
                                    category = it.category,
                                    note = it.note,
                                    dateText = it.dateText,
                                    timeText = it.timeText,
                                )
                            }
                            onSave(result)
                        },
                    ) {
                        Text(
                            text = "保存${if (editableItems.isNotEmpty()) " (${editableItems.size}条)" else ""}",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseEditCard(
    item: EditableItem,
    categories: List<String>,
    onUpdate: (EditableItem) -> Unit,
    onDelete: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var amountInput by remember(item.uid) { mutableStateOf(item.amountText) }
    var noteInput by remember(item.uid) { mutableStateOf(item.note) }
    var dateInput by remember(item.uid) { mutableStateOf(item.dateText) }
    var timeInput by remember(item.uid) { mutableStateOf(item.timeText) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF6)),
        border = BorderStroke(1.dp, Color(0xFFE9DFCF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "支出 ${item.category.ifBlank { "未分类" }}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "删除此项",
                        tint = Color(0xFFB3261E),
                    )
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { newValue ->
                    amountInput = newValue
                    onUpdate(item.copy(amountText = newValue))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("金额") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1D6C7)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.category.ifBlank { "选择分类" },
                            color = if (item.category.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(
                            text = "选择",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                categoryExpanded = false
                                onUpdate(item.copy(category = cat))
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = noteInput,
                onValueChange = { newValue ->
                    noteInput = newValue
                    onUpdate(item.copy(note = newValue))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { newValue ->
                        dateInput = newValue
                        onUpdate(item.copy(dateText = newValue))
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("日期") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { newValue ->
                        timeInput = newValue
                        onUpdate(item.copy(timeText = newValue))
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("时间") },
                    singleLine = true,
                )
            }
        }
    }
}
