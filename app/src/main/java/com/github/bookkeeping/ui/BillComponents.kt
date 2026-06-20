package com.github.bookkeeping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.bookkeeping.CategoryCatalog
import com.github.bookkeeping.Direction
import com.github.bookkeeping.R
import com.github.bookkeeping.ReviewStatus
import com.github.bookkeeping.SourceKey
import com.github.bookkeeping.data.BillEntity

@Composable
internal fun DayHeader(label: String, expense: Long, income: Long) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.day_total_format, amountText(expense), amountText(income)),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF68736F)
            )
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = Color(0xFFE5E8EC))
    }
}

@Composable
internal fun BillRow(bill: BillEntity, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val category = stringResource(CategoryCatalog.labelRes(bill.categoryKey, bill.direction))
        BoxedCategoryInitial(category, bill.direction)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (bill.sourceChannel != SourceKey.MANUAL) {
                    Spacer(Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.source_auto_badge, sourceName(bill.sourceChannel)), maxLines = 1) }
                    )
                }
                if (bill.reviewStatus == ReviewStatus.REVIEW) {
                    Spacer(Modifier.width(8.dp))
                    SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.review_badge)) })
                }
            }
            val meta = listOfNotNull(
                timeText(bill.transactionTime),
                bill.merchant.takeIf { it.isNotBlank() && !isSourceKey(it) } ?: sourceNameOrNull(bill.merchant),
                bill.note.takeIf { it.isNotBlank() }
            ).joinToString("  ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = Color(0xFF66706D), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            (if (bill.direction == Direction.EXPENSE) "-" else "+") + amountText(bill.amountCents),
            style = MaterialTheme.typography.titleMedium,
            color = if (bill.direction == Direction.EXPENSE) Color(0xFF202625) else Color(0xFF2E7D74),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BoxedCategoryInitial(category: String, direction: String) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (direction == Direction.EXPENSE) Color(0xFFFFF0E9) else Color(0xFFEAF6EF)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            category.take(1),
            color = if (direction == Direction.EXPENSE) Color(0xFFE07A5F) else Color(0xFF2E7D74),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun BillDetailDialog(bill: BillEntity, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bill_detail)) },
        text = {
            Column {
                DetailLine(stringResource(R.string.field_amount), (if (bill.direction == Direction.EXPENSE) "-" else "+") + amountText(bill.amountCents))
                DetailLine(stringResource(R.string.field_category), stringResource(CategoryCatalog.labelRes(bill.categoryKey, bill.direction)))
                DetailLine(stringResource(R.string.field_merchant), bill.merchant.takeIf { it.isNotBlank() && !isSourceKey(it) } ?: sourceName(bill.sourceChannel))
                DetailLine(stringResource(R.string.field_time), dateTimeText(bill.transactionTime))
                DetailLine(stringResource(R.string.field_source), sourceName(bill.sourceChannel))
                if (bill.note.isNotBlank()) DetailLine(stringResource(R.string.field_note), bill.note)
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Default.Edit, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_edit))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_delete))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Color(0xFF65706D), modifier = Modifier.width(92.dp))
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}
