package com.example.bookkeeping.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bookkeeping.BookkeepingRepository
import com.example.bookkeeping.CategoryKey
import com.example.bookkeeping.Direction
import com.example.bookkeeping.R
import com.example.bookkeeping.data.BillEntity
import kotlinx.coroutines.launch

private data class LedgerTab(
    val route: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector
)

private val ledgerTabs = listOf(
    LedgerTab("home", R.string.tab_bookkeeping, Icons.AutoMirrored.Filled.ReceiptLong),
    LedgerTab("reports", R.string.tab_reports, Icons.Default.PieChart),
    LedgerTab("add", R.string.tab_add, Icons.Default.AddCircle),
    LedgerTab("automation", R.string.tab_automation, Icons.Default.AutoAwesome),
    LedgerTab("settings", R.string.tab_settings, Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp(repository: BookkeepingRepository) {
    val context = LocalContext.current
    val accessibilityFixtureCreated = stringResource(R.string.accessibility_fixture_created)
    val amountInvalid = stringResource(R.string.amount_invalid)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    // bills 由数据库 Flow 驱动：任何插入/更新/删除都会自动刷新 UI，无需手动 reload。
    val bills by repository.billsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by rememberSaveable(stateSaver = BillDraft.Saver) { mutableStateOf<BillDraft?>(null) }
    var detailBill by remember { mutableStateOf<BillEntity?>(null) }
    var deleteBill by remember { mutableStateOf<BillEntity?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toast = null
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = Color.White
            ) {
                ledgerTabs.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route || (item.route == "home" && route == "search"),
                        onClick = {
                            if (item.route == "add") {
                                draft = BillDraft()
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.titleRes)) },
                        label = { Text(stringResource(item.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    bills = bills,
                    onSearch = { navController.navigate("search") },
                    onReports = { navController.navigate("reports") },
                    onAdd = { draft = BillDraft() },
                    onBillClick = { detailBill = it }
                )
            }
            composable("reports") {
                ReportsScreen(bills = bills)
            }
            composable("automation") {
                AutomationScreen(
                    onFixture = { navController.navigate("fixture") },
                    onToast = { toast = it },
                    onReload = {}
                )
            }
            composable("settings") {
                SettingsScreen(
                    repository = repository,
                    onAutomation = { navController.navigate("automation") },
                    onCategories = { navController.navigate("categories") },
                    onImportDone = {
                        toast = it
                    }
                )
            }
            composable("search") {
                SearchScreen(
                    bills = bills,
                    onBack = { navController.popBackStack() },
                    onBillClick = { detailBill = it }
                )
            }
            composable("categories") {
                CategoryManagementScreen(onBack = { navController.popBackStack() })
            }
            composable("fixture") {
                FixtureScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        toast = accessibilityFixtureCreated
                    }
                )
            }
        }
    }

    draft?.let { current ->
        AddBillSheet(
            initial = current,
            onDismiss = { draft = null },
            onSave = { saved, keepOpen ->
                scope.launch {
                    val amount = parseAmountCents(saved.amount)
                    if (amount == null || amount <= 0L) {
                        toast = amountInvalid
                    } else {
                        repository.saveManual(
                            existingId = saved.existingId,
                            amountCents = amount,
                            direction = saved.direction,
                            categoryKey = saved.categoryKey,
                            merchant = saved.merchant,
                            account = saved.account,
                            note = saved.note,
                            transactionTime = saved.transactionTime
                        )
                        draft = if (keepOpen) {
                            BillDraft(direction = saved.direction, categoryKey = saved.categoryKey)
                        } else {
                            null
                        }
                    }
                }
            }
        )
    }

    detailBill?.let { bill ->
        BillDetailDialog(
            bill = bill,
            onDismiss = { detailBill = null },
            onEdit = {
                detailBill = null
                draft = BillDraft(
                    existingId = bill.id,
                    direction = bill.direction,
                    amount = amountText(bill.amountCents),
                    categoryKey = bill.categoryKey,
                    merchant = bill.merchant,
                    account = bill.account,
                    note = bill.note,
                    transactionTime = bill.transactionTime
                )
            },
            onDelete = {
                detailBill = null
                deleteBill = bill
            }
        )
    }

    deleteBill?.let { bill ->
        AlertDialog(
            onDismissRequest = { deleteBill = null },
            title = { Text(stringResource(R.string.delete_bill_title)) },
            text = { Text(stringResource(R.string.delete_bill_desc)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.delete(bill.id)
                        deleteBill = null
                    }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteBill = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
