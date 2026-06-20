package com.github.bookkeeping.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.bookkeeping.CategoryCatalog
import com.github.bookkeeping.CategoryKey
import com.github.bookkeeping.Direction
import com.github.bookkeeping.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddBillSheet(initial: BillDraft, onDismiss: () -> Unit, onSave: (BillDraft, keepOpen: Boolean) -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics { testTagsAsResourceId = true }
                .testTag(TAG_ADD_BILL_SHEET)
                .padding(horizontal = 20.dp)
                .padding(bottom = bottomInteractivePadding())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close)) }
                Spacer(Modifier.weight(1f))
                DirectionChip(R.string.direction_expense, draft.direction == Direction.EXPENSE) {
                    draft = draft.copy(direction = Direction.EXPENSE, categoryKey = CategoryKey.FOOD)
                }
                Spacer(Modifier.width(8.dp))
                DirectionChip(R.string.direction_income, draft.direction == Direction.INCOME) {
                    draft = draft.copy(direction = Direction.INCOME, categoryKey = CategoryKey.RECEIPT)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.currency_symbol), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(
                    draft.amount.ifBlank { stringResource(R.string.field_amount) },
                    style = MaterialTheme.typography.displaySmall,
                    color = if (draft.amount.isBlank()) Color(0xFFB8C0BC) else Color(0xFF202625),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = draft.transactionTime }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                            draft = draft.copy(transactionTime = calendar.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }, label = { Text(dateShortText(draft.transactionTime)) })
                AssistChip(onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = draft.transactionTime }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            draft = draft.copy(transactionTime = calendar.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                }, label = { Text(timeText(draft.transactionTime)) })
            }
            Spacer(Modifier.height(10.dp))
            CategoryGrid(draft.direction, draft.categoryKey) { draft = draft.copy(categoryKey = it) }
            OutlinedTextField(
                value = draft.merchant,
                onValueChange = { draft = draft.copy(merchant = it) },
                label = { Text(stringResource(R.string.field_merchant)) },
                placeholder = { Text(stringResource(R.string.merchant_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft.account,
                    onValueChange = { draft = draft.copy(account = it) },
                    label = { Text(stringResource(R.string.field_account)) },
                    placeholder = { Text(stringResource(R.string.account_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text(stringResource(R.string.field_note)) },
                    placeholder = { Text(stringResource(R.string.remark_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(10.dp))
            NumberKeyboard(
                onKey = { key ->
                    draft = when (key) {
                        "delete" -> draft.copy(amount = draft.amount.dropLast(1))
                        "." -> if (draft.amount.contains('.')) draft else draft.copy(amount = draft.amount.ifBlank { "0" } + ".")
                        "save" -> {
                            onSave(draft, false)
                            draft
                        }
                        "again" -> {
                            onSave(draft, true)
                            if ((parseAmountCents(draft.amount) ?: 0L) > 0L) {
                                BillDraft(direction = draft.direction, categoryKey = draft.categoryKey)
                            } else {
                                draft
                            }
                        }
                        "+", "-" -> draft
                        else -> {
                            val next = (draft.amount + key).take(12)
                            draft.copy(amount = normalizeAmountInput(next))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DirectionChip(@StringRes text: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(text)) })
}

@Composable
private fun CategoryGrid(direction: String, selected: String, onSelect: (String) -> Unit) {
    val categories = CategoryCatalog.forDirection(direction).take(10)
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
    ) {
        items(categories) { category ->
            val label = stringResource(category.labelRes)
            Column(
                Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(category.key) }
                    .background(if (selected == category.key) Color(0xFFEAF6EF) else Color.Transparent)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (selected == category.key) Color(0xFF2E7D74) else Color(0xFFEAF1F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label.take(1), color = if (selected == category.key) Color.White else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NumberKeyboard(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "delete"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("again", "0", ".", "save")
    )
    Column(Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val label = when (key) {
                        "delete" -> stringResource(R.string.keyboard_delete)
                        "again" -> stringResource(R.string.tab_add)
                        "save" -> stringResource(R.string.action_save)
                        else -> key
                    }
                    Button(
                        onClick = { onKey(key) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .padding(2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (key == "save") MaterialTheme.colorScheme.primary else Color(0xFFF2F4F3),
                            contentColor = if (key == "save") Color.White else Color(0xFF202625)
                        )
                    ) {
                        when (key) {
                            "delete" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = label)
                            "again" -> Icon(Icons.Default.Add, contentDescription = label)
                            "save" -> Icon(Icons.Default.Check, contentDescription = label)
                            else -> Text(label, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
