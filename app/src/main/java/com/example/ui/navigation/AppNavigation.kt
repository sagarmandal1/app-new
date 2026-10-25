package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppStrings
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expenses.AddExpenseDialog
import com.example.ui.screens.ledger.LedgerScreen
import com.example.ui.screens.purchases.AddPurchaseDialog
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.sales.CreateSaleDialog
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.screens.security.PinLockScreen
import com.example.ui.screens.services.ServicesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.viewmodel.BizViewModel

enum class Screen(val titleKey: String, val icon: ImageVector) {
    DASHBOARD("nav_dashboard", Icons.Default.Dashboard),
    SALES("nav_sales", Icons.Default.ReceiptLong),
    SERVICES("nav_services", Icons.Default.BusinessCenter),
    LEDGER("nav_ledger", Icons.Default.AccountBalanceWallet),
    REPORTS("nav_reports", Icons.AutoMirrored.Filled.TrendingUp),
    SETTINGS("nav_settings", Icons.Default.Settings)
}

@Composable
fun MainAppContainer(
    viewModel: BizViewModel
) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val services by viewModel.services.collectAsState()
    val activeServices by viewModel.activeServices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    val lang = settings.language

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    // Quick Action Dialog states
    var showAddSaleDialog by remember { mutableStateOf(false) }
    var showAddPurchaseDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        PinLockScreen(
            businessName = settings.businessName,
            onUnlock = { pin -> viewModel.unlockWithPin(pin) }
        )
        return
    }

    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val navItems = listOf(
                    Screen.DASHBOARD,
                    Screen.SALES,
                    Screen.SERVICES,
                    Screen.LEDGER,
                    Screen.REPORTS,
                    Screen.SETTINGS
                )
                navItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = AppStrings.get(screen.titleKey, lang)
                            )
                        },
                        label = {
                            Text(
                                text = AppStrings.get(screen.titleKey, lang),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSales = { currentScreen = Screen.SALES },
                    onOpenAddSale = { showAddSaleDialog = true },
                    onOpenAddPurchase = { showAddPurchaseDialog = true },
                    onOpenAddExpense = { showAddExpenseDialog = true },
                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                    onLockApp = { viewModel.lock() }
                )
                Screen.SALES -> SalesScreen(
                    viewModel = viewModel
                )
                Screen.SERVICES -> ServicesScreen(
                    viewModel = viewModel
                )
                Screen.LEDGER -> LedgerScreen(
                    viewModel = viewModel
                )
                Screen.REPORTS -> ReportsScreen(
                    viewModel = viewModel
                )
                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Global Add Sale Dialog
    if (showAddSaleDialog) {
        CreateSaleDialog(
            activeServices = activeServices,
            customers = customers,
            paymentMethods = settings.paymentMethods,
            currency = settings.currency,
            lang = lang,
            onDismiss = { showAddSaleDialog = false },
            onSaveCustomer = { newCust, onCreated ->
                viewModel.saveCustomer(newCust) { id -> onCreated(id) }
            },
            onSaveSale = { custId, custName, srv, qty, sellPrice, disc, paid, method, note ->
                viewModel.recordSale(
                    customerId = custId,
                    customerName = custName,
                    service = srv,
                    quantity = qty,
                    unitSellPrice = sellPrice,
                    discount = disc,
                    paidAmount = paid,
                    paymentMethod = method,
                    note = note
                ) {
                    showAddSaleDialog = false
                    currentScreen = Screen.SALES
                }
            }
        )
    }

    // Global Add Purchase Dialog
    if (showAddPurchaseDialog) {
        AddPurchaseDialog(
            services = services,
            suppliers = suppliers,
            paymentMethods = settings.paymentMethods,
            currency = settings.currency,
            lang = lang,
            onDismiss = { showAddPurchaseDialog = false },
            onQuickAddSupplier = { newSup, onCreated ->
                viewModel.saveSupplier(newSup) { id -> onCreated(id) }
            },
            onSavePurchase = { supId, supName, srvId, srvName, qty, buyPrice, paid, method, note ->
                viewModel.recordPurchase(
                    supplierId = supId,
                    supplierName = supName,
                    serviceId = srvId,
                    serviceName = srvName,
                    quantity = qty,
                    unitBuyPrice = buyPrice,
                    paidAmount = paid,
                    paymentMethod = method,
                    note = note
                ) {
                    showAddPurchaseDialog = false
                    currentScreen = Screen.LEDGER
                }
            }
        )
    }

    // Global Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            expenseCategories = settings.expenseCategories,
            paymentMethods = settings.paymentMethods,
            currency = settings.currency,
            lang = lang,
            onDismiss = { showAddExpenseDialog = false },
            onSaveExpense = { title, cat, amt, method, date, note ->
                viewModel.recordExpense(title, cat, amt, method, date, note) {
                    showAddExpenseDialog = false
                }
            }
        )
    }
}
