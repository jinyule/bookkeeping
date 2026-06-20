package com.github.bookkeeping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.bookkeeping.Direction
import com.github.bookkeeping.R
import com.github.bookkeeping.data.BillEntity

@Composable
internal fun HomeScreen(
    bills: List<BillEntity>,
    onSearch: () -> Unit,
    onReports: () -> Unit,
    onAdd: () -> Unit,
    onBillClick: (BillEntity) -> Unit
) {
    val todayExpense = remember(bills) {
        bills.filter { it.direction == Direction.EXPENSE && isToday(it.transactionTime) }.sumOf { it.amountCents }
    }
    val monthExpense = remember(bills) {
        bills.filter { it.direction == Direction.EXPENSE && isThisMonth(it.transactionTime) }.sumOf { it.amountCents }
    }
    val monthIncome = remember(bills) {
        bills.filter { it.direction == Direction.INCOME && isThisMonth(it.transactionTime) }.sumOf { it.amountCents }
    }

    val groups = remember(bills) { if (bills.isEmpty()) emptyList() else groupedBills(bills) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8F3F2), Color(0xFFF7F8FA))
                    )
                )
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                TopBar(
                    title = stringResource(R.string.tab_bookkeeping),
                    actions = {
                        TextButton(onClick = onReports) { Text(stringResource(R.string.home_daily_report)) }
                        IconButton(onClick = onSearch, modifier = Modifier.testTag("action_search")) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                        }
                        IconButton(onClick = onAdd) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit)) }
                    }
                )
            }
            item {
                SummaryCard(todayExpense, monthExpense, monthIncome)
            }
            item {
                BudgetPromptCard()
            }
            if (bills.isEmpty()) {
                item {
                    EmptyBills(onAdd)
                }
            } else {
                groups.forEach { group ->
                    item {
                        DayHeader(group.dateLabel, group.expense, group.income)
                    }
                    items(group.items, key = { it.id }) { bill ->
                        BillRow(bill = bill, onClick = { onBillClick(bill) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(todayExpense: Long, monthExpense: Long, monthIncome: Long) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF2E7D74)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(stringResource(R.string.today_expense), color = Color.White.copy(alpha = 0.82f))
            Text(
                amountText(todayExpense),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryMetric(stringResource(R.string.month_expense), amountText(monthExpense), Modifier.weight(1f))
                SummaryMetric(stringResource(R.string.month_income), amountText(monthIncome), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BudgetPromptCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF1F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.budget_prompt), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyBills(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF1F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.empty_bills_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_bills_desc), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64706C))
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tab_add))
        }
    }
}
