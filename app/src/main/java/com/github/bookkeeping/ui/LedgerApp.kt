package com.github.bookkeeping.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.systemGestureExclusion
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.bookkeeping.BookkeepingRepository
import com.github.bookkeeping.CategoryKey
import com.github.bookkeeping.Direction
import com.github.bookkeeping.R
import com.github.bookkeeping.data.BillEntity
import kotlinx.coroutines.launch

private data class LedgerTab(
    val route: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val testTag: String
)

private const val ROUTE_HOME = "home"
private const val ROUTE_REPORTS = "reports"
private const val ROUTE_ADD = "add"
private const val ROUTE_AUTOMATION = "automation"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_CATEGORIES = "categories"
private const val ROUTE_FIXTURE = "fixture"

private const val TAG_BOTTOM_BAR = "ledger_bottom_bar"
private const val TAG_NAV_HOME = "nav_home"
private const val TAG_NAV_REPORTS = "nav_reports"
private const val TAG_NAV_ADD = "nav_add"
private const val TAG_NAV_AUTOMATION = "nav_automation"
private const val TAG_NAV_SETTINGS = "nav_settings"
private const val TAG_SCREEN_HOME = "screen_home"
private const val TAG_SCREEN_REPORTS = "screen_reports"
private const val TAG_SCREEN_AUTOMATION = "screen_automation"
private const val TAG_SCREEN_SETTINGS = "screen_settings"
private const val TAG_SCREEN_SEARCH = "screen_search"
private const val TAG_SCREEN_CATEGORIES = "screen_categories"
private const val TAG_SCREEN_FIXTURE = "screen_fixture"
internal const val TAG_ADD_BILL_SHEET = "add_bill_sheet"

private val ledgerTabs = listOf(
    LedgerTab(ROUTE_HOME, R.string.tab_bookkeeping, Icons.AutoMirrored.Filled.ReceiptLong, TAG_NAV_HOME),
    LedgerTab(ROUTE_REPORTS, R.string.tab_reports, Icons.Default.PieChart, TAG_NAV_REPORTS),
    LedgerTab(ROUTE_ADD, R.string.tab_add, Icons.Default.AddCircle, TAG_NAV_ADD),
    LedgerTab(ROUTE_AUTOMATION, R.string.tab_automation, Icons.Default.AutoAwesome, TAG_NAV_AUTOMATION),
    LedgerTab(ROUTE_SETTINGS, R.string.tab_settings, Icons.Default.Settings, TAG_NAV_SETTINGS)
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
    val route = backStack?.destination?.route ?: ROUTE_HOME
    val selectedRoute = selectedTopLevelRoute(route)

    fun navigateToTopLevel(destination: String) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            LedgerBottomBar(
                selectedRoute = selectedRoute,
                onTabClick = { item ->
                    if (item.route == ROUTE_ADD) {
                        draft = BillDraft()
                    } else {
                        navigateToTopLevel(item.route)
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_HOME) {
                ScreenBox(TAG_SCREEN_HOME) {
                    HomeScreen(
                        bills = bills,
                        onSearch = { navController.navigate(ROUTE_SEARCH) },
                        onReports = { navigateToTopLevel(ROUTE_REPORTS) },
                        onAdd = { draft = BillDraft() },
                        onBillClick = { detailBill = it }
                    )
                }
            }
            composable(ROUTE_REPORTS) {
                ScreenBox(TAG_SCREEN_REPORTS) {
                    ReportsScreen(bills = bills)
                }
            }
            composable(ROUTE_AUTOMATION) {
                ScreenBox(TAG_SCREEN_AUTOMATION) {
                    AutomationScreen(
                        onFixture = { navController.navigate(ROUTE_FIXTURE) },
                        onToast = { toast = it },
                        onReload = {}
                    )
                }
            }
            composable(ROUTE_SETTINGS) {
                ScreenBox(TAG_SCREEN_SETTINGS) {
                    SettingsScreen(
                        repository = repository,
                        onAutomation = { navigateToTopLevel(ROUTE_AUTOMATION) },
                        onCategories = { navController.navigate(ROUTE_CATEGORIES) },
                        onImportDone = {
                            toast = it
                        }
                    )
                }
            }
            composable(ROUTE_SEARCH) {
                ScreenBox(TAG_SCREEN_SEARCH) {
                    SearchScreen(
                        bills = bills,
                        onBack = { navController.popBackStack() },
                        onBillClick = { detailBill = it }
                    )
                }
            }
            composable(ROUTE_CATEGORIES) {
                ScreenBox(TAG_SCREEN_CATEGORIES) {
                    CategoryManagementScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(ROUTE_FIXTURE) {
                ScreenBox(TAG_SCREEN_FIXTURE) {
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

@Composable
private fun LedgerBottomBar(
    selectedRoute: String,
    onTabClick: (LedgerTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .systemGestureExclusion()
            .testTag(TAG_BOTTOM_BAR)
    ) {
        NavigationBar(
            containerColor = Color.White,
            windowInsets = WindowInsets(0.dp)
        ) {
            ledgerTabs.forEach { item ->
                NavigationBarItem(
                    selected = selectedRoute == item.route,
                    onClick = { onTabClick(item) },
                    modifier = Modifier.testTag(item.testTag),
                    icon = { Icon(item.icon, contentDescription = stringResource(item.titleRes)) },
                    label = { Text(stringResource(item.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(bottomInteractivePadding())
        )
    }
}

@Composable
private fun ScreenBox(testTag: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag)
    ) {
        content()
    }
}

private fun selectedTopLevelRoute(route: String): String = when (route) {
    ROUTE_SEARCH -> ROUTE_HOME
    ROUTE_CATEGORIES -> ROUTE_SETTINGS
    ROUTE_FIXTURE -> ROUTE_AUTOMATION
    else -> route
}

@Composable
internal fun bottomInteractivePadding(): Dp {
    val density = LocalDensity.current
    val bottomInset = maxOf(
        WindowInsets.navigationBars.getBottom(density),
        WindowInsets.tappableElement.getBottom(density),
        WindowInsets.systemGestures.getBottom(density),
        WindowInsets.mandatorySystemGestures.getBottom(density)
    )
    val reportedPadding = with(density) { bottomInset.toDp() }
    return if (reportedPadding > 0.dp) reportedPadding else 80.dp
}
