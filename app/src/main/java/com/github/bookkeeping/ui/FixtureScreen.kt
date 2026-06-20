package com.github.bookkeeping.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.bookkeeping.BookkeepingRepository
import com.github.bookkeeping.R
import com.github.bookkeeping.recognition.PaymentTextParser
import kotlinx.coroutines.launch

@Composable
internal fun FixtureScreen(repository: BookkeepingRepository, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fixtureCreated = stringResource(R.string.accessibility_fixture_created)
    val samples = listOf(
        stringResource(R.string.fixture_wechat_expense),
        stringResource(R.string.fixture_wechat_income),
        stringResource(R.string.fixture_promo)
    )
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item { TopBar(title = stringResource(R.string.fixture_title), onBack = onBack) }
        item {
            CardBlock {
                Text(stringResource(R.string.fixture_desc), color = Color(0xFF65706D))
                Spacer(Modifier.height(12.dp))
                samples.forEach { sample ->
                    Text(sample, modifier = Modifier.padding(vertical = 8.dp))
                    HorizontalDivider()
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    scope.launch {
                        samples.forEach { sample ->
                            PaymentTextParser.parseAccessibilityText("com.tencent.mm", sample)?.let {
                                repository.acceptCandidate(it)
                            }
                        }
                        Toast.makeText(context, fixtureCreated, Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                }) {
                    Text(stringResource(R.string.action_parse_fixture))
                }
            }
        }
    }
}
