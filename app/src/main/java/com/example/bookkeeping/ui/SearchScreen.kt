package com.example.bookkeeping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bookkeeping.R
import com.example.bookkeeping.data.BillEntity

@Composable
internal fun SearchScreen(bills: List<BillEntity>, onBack: () -> Unit, onBillClick: (BillEntity) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, bills) {
        if (query.isBlank()) bills else bills.filter {
            it.merchant.contains(query, true) ||
                it.note.contains(query, true) ||
                it.categoryKey.contains(query, true) ||
                it.sourceChannel.contains(query, true)
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(title = stringResource(R.string.search_title), onBack = onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            singleLine = true
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (results.isEmpty()) {
                item { Text(stringResource(R.string.search_no_result), Modifier.padding(24.dp), color = Color(0xFF65706D)) }
            } else {
                items(results, key = { it.id }) { BillRow(it) { onBillClick(it) } }
            }
        }
    }
}
