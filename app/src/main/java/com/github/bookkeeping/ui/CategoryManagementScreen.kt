package com.github.bookkeeping.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.github.bookkeeping.CategoryDef
import com.github.bookkeeping.R

@Composable
internal fun CategoryManagementScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item { TopBar(title = stringResource(R.string.category_management_title), onBack = onBack) }
        item {
            Text(
                stringResource(R.string.category_management_tip),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = Color(0xFF65706D),
                style = MaterialTheme.typography.bodySmall
            )
        }
        item { CategorySection(R.string.category_expense_management, CategoryCatalog.expense) }
        item { CategorySection(R.string.category_income_management, CategoryCatalog.income) }
    }
}

@Composable
private fun CategorySection(@StringRes title: Int, categories: List<CategoryDef>) {
    Text(
        stringResource(title),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height((((categories.size + 4) / 5) * 86).dp)
            .padding(horizontal = 14.dp)
    ) {
        items(categories) { category ->
            Column(
                Modifier
                    .padding(4.dp)
                    .aspectRatio(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val label = stringResource(category.labelRes)
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAF1F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label.take(1), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
