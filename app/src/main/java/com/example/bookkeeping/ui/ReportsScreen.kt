package com.example.bookkeeping.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bookkeeping.CategoryCatalog
import com.example.bookkeeping.Direction
import com.example.bookkeeping.R
import com.example.bookkeeping.data.BillEntity
import kotlin.math.max

private enum class ReportRange { WEEK, MONTH, YEAR }

@Composable
internal fun ReportsScreen(bills: List<BillEntity>) {
    var range by rememberSaveable { mutableStateOf(ReportRange.MONTH) }
    val inRange: (Long) -> Boolean = when (range) {
        ReportRange.WEEK -> { t -> isThisWeek(t) }
        ReportRange.MONTH -> { t -> isThisMonth(t) }
        ReportRange.YEAR -> { t -> isThisYear(t) }
    }
    val expenseBills = remember(bills, range) {
        bills.filter { it.direction == Direction.EXPENSE && inRange(it.transactionTime) }
    }
    val incomeBills = remember(bills, range) {
        bills.filter { it.direction == Direction.INCOME && inRange(it.transactionTime) }
    }
    val rangeExpense = expenseBills.sumOf { it.amountCents }
    val rangeIncome = incomeBills.sumOf { it.amountCents }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { TopBar(title = stringResource(R.string.report_title)) }
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    R.string.report_week to ReportRange.WEEK,
                    R.string.report_month to ReportRange.MONTH,
                    R.string.report_year to ReportRange.YEAR
                ).forEach { (res, r) ->
                    FilterChip(
                        selected = range == r,
                        onClick = { range = r },
                        label = { Text(stringResource(res)) }
                    )
                }
            }
        }
        item {
            CardBlock {
                Row(Modifier.fillMaxWidth()) {
                    ReportMetric(stringResource(R.string.month_expense), amountText(rangeExpense), Modifier.weight(1f))
                    ReportMetric(
                        stringResource(R.string.report_average_expense),
                        amountText(if (range == ReportRange.MONTH && expenseBills.isNotEmpty()) rangeExpense / max(1, currentDayOfMonth()) else 0),
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth()) {
                    ReportMetric(stringResource(R.string.month_income), amountText(rangeIncome), Modifier.weight(1f))
                    ReportMetric(stringResource(R.string.report_balance), amountText(rangeIncome - rangeExpense), Modifier.weight(1f))
                }
            }
        }
        item {
            ChartCard(title = stringResource(R.string.report_expense_trend), bills = expenseBills)
        }
        item {
            CategoryBreakdownCard(expenseBills)
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(4.dp)) {
        Text(label, color = Color(0xFF65706D), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChartCard(title: String, bills: List<BillEntity>) {
    CardBlock {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        if (bills.isEmpty()) {
            Text(stringResource(R.string.report_no_data), color = Color(0xFF65706D))
        } else {
            val values = dailyExpenseValues(bills)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
                val stepX = size.width / max(1, values.size - 1)
                val points = values.mapIndexed { index, value ->
                    Offset(index * stepX, size.height - (value.toFloat() / maxValue.toFloat()) * size.height)
                }
                for (i in 0 until points.lastIndex) {
                    drawLine(Color(0xFF2E7D74), points[i], points[i + 1], strokeWidth = 5f, cap = StrokeCap.Round)
                }
                points.forEach { drawCircle(Color(0xFFE07A5F), radius = 6f, center = it) }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(bills: List<BillEntity>) {
    CardBlock {
        Text(stringResource(R.string.report_category_breakdown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        if (bills.isEmpty()) {
            Text(stringResource(R.string.report_no_data), color = Color(0xFF65706D))
        } else {
            val totals = bills.groupBy { it.categoryKey }.mapValues { it.value.sumOf { bill -> bill.amountCents } }
            val total = totals.values.sum().coerceAtLeast(1L)
            totals.entries.sortedByDescending { it.value }.take(6).forEach { entry ->
                val label = stringResource(CategoryCatalog.labelRes(entry.key))
                val fraction = entry.value.toFloat() / total.toFloat()
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.width(82.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Canvas(Modifier.weight(1f).height(10.dp)) {
                        drawLine(Color(0xFFE5E8EC), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 10f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF2E7D74), Offset(0f, size.height / 2), Offset(size.width * fraction, size.height / 2), strokeWidth = 10f, cap = StrokeCap.Round)
                    }
                    Text("${(fraction * 100).toInt()}%", Modifier.width(44.dp).padding(start = 8.dp), color = Color(0xFF65706D))
                }
            }
        }
    }
}
