package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DuePaymentEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.ServiceEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.model.BackupRestoreSummary
import com.example.data.model.BusinessSettings
import com.example.data.repository.BizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardSummary(
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val todayProfit: Double = 0.0,
    val totalCustomerDue: Double = 0.0,
    val totalSupplierDue: Double = 0.0,
    val totalSalesAmount: Double = 0.0,
    val totalNetProfit: Double = 0.0,
    val monthlyChartData: List<MonthlyDataPoint> = emptyList(),
    val topServices: List<TopServiceStat> = emptyList(),
    val recentTransactions: List<RecentTransaction> = emptyList()
)

data class MonthlyDataPoint(
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val profit: Double
)

data class TopServiceStat(
    val serviceName: String,
    val serviceType: String,
    val totalSalesCount: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class RecentTransaction(
    val id: Long,
    val type: String, // "SALE", "PURCHASE", "EXPENSE", "DUE_COLLECTION", "DUE_PAYMENT"
    val title: String,
    val subtitle: String,
    val amount: Double,
    val profit: Double = 0.0,
    val isIncome: Boolean,
    val date: Long,
    val paymentMethod: String
)

data class PartyLedgerInfo(
    val totalBilled: Double,
    val totalPaid: Double,
    val balanceDue: Double
)

class BizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BizRepository = BizRepository(AppDatabase.getDatabase(application))

    val services: StateFlow<List<ServiceEntity>> = repository.services
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeServices: StateFlow<List<ServiceEntity>> = repository.activeServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.suppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.sales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseEntity>> = repository.purchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duePayments: StateFlow<List<DuePaymentEntity>> = repository.duePayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<BusinessSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettings())

    private val _isUnlocked = MutableStateFlow(true)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                if (s.pinEnabled && s.pinCode.isNotBlank()) {
                    // Start locked if PIN security is on
                    if (_isUnlocked.value) {
                        _isUnlocked.value = false
                    }
                } else {
                    _isUnlocked.value = true
                }
            }
        }
    }

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        sales, purchases, expenses, duePayments
    ) { salesList, purchasesList, expensesList, duePaymentsList ->
        computeDashboardSummary(salesList, purchasesList, expensesList, duePaymentsList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private fun computeDashboardSummary(
        salesList: List<SaleEntity>,
        purchasesList: List<PurchaseEntity>,
        expensesList: List<ExpenseEntity>,
        duePaymentsList: List<DuePaymentEntity>
    ): DashboardSummary {
        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Today's collections from sales + customer due payments
        val todaySalesIncome = salesList.filter { it.date >= startOfToday }.sumOf { it.paidAmount }
        val todayCustDueCollected = duePaymentsList.filter { it.partyType == "CUSTOMER" && it.date >= startOfToday }.sumOf { it.amount }
        val todayIncome = todaySalesIncome + todayCustDueCollected

        // Today's expenses + purchases payments + supplier due paid
        val todayDirectExpenses = expensesList.filter { it.date >= startOfToday }.sumOf { it.amount }
        val todayPurchasesPaid = purchasesList.filter { it.date >= startOfToday }.sumOf { it.paidAmount }
        val todaySuppDuePaid = duePaymentsList.filter { it.partyType == "SUPPLIER" && it.date >= startOfToday }.sumOf { it.amount }
        val todayExpense = todayDirectExpenses + todayPurchasesPaid + todaySuppDuePaid

        // Today's Sales Profit - Today's direct expenses
        val todaySalesProfit = salesList.filter { it.date >= startOfToday }.sumOf { it.profit }
        val todayProfit = todaySalesProfit - todayDirectExpenses

        // Total Customer Due to Collect = (Sum of all sales dues) - (Sum of direct customer due payments)
        val grossCustomerDue = salesList.sumOf { it.dueAmount }
        val directCustPayments = duePaymentsList.filter { it.partyType == "CUSTOMER" }.sumOf { it.amount }
        val totalCustomerDue = maxOf(0.0, grossCustomerDue - directCustPayments)

        // Total Supplier Due to Pay = (Sum of all purchase dues) - (Sum of direct supplier due payments)
        val grossSupplierDue = purchasesList.sumOf { it.dueAmount }
        val directSuppPayments = duePaymentsList.filter { it.partyType == "SUPPLIER" }.sumOf { it.amount }
        val totalSupplierDue = maxOf(0.0, grossSupplierDue - directSuppPayments)

        // Overall Sales and Net Profit
        val totalSalesAmount = salesList.sumOf { it.totalAmount }
        val totalSalesProfit = salesList.sumOf { it.profit }
        val totalExpensesAmount = expensesList.sumOf { it.amount }
        val totalNetProfit = totalSalesProfit - totalExpensesAmount

        // Monthly Breakdown for Chart (Last 6 Months)
        val monthlyPoints = mutableListOf<MonthlyDataPoint>()
        val cal = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        for (i in 5 downTo 0) {
            val targetCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val y = targetCal.get(Calendar.YEAR)
            val m = targetCal.get(Calendar.MONTH)
            val label = monthNames[m]

            val mSales = salesList.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.date }
                c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
            }
            val mExpenses = expensesList.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.date }
                c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
            }

            val mIncome = mSales.sumOf { it.paidAmount }
            val mExp = mExpenses.sumOf { it.amount }
            val mProfit = mSales.sumOf { it.profit } - mExp

            monthlyPoints.add(MonthlyDataPoint(label, mIncome, mExp, mProfit))
        }

        // Top Performing Services
        val topMap = mutableMapOf<String, Pair<String, Triple<Int, Double, Double>>>()
        salesList.forEach { s ->
            val prev = topMap[s.serviceName]
            val qty = (prev?.second?.first ?: 0) + s.quantity
            val rev = (prev?.second?.second ?: 0.0) + s.totalAmount
            val prf = (prev?.second?.third ?: 0.0) + s.profit
            topMap[s.serviceName] = Pair(s.serviceType, Triple(qty, rev, prf))
        }
        val topServices = topMap.map { (name, info) ->
            TopServiceStat(
                serviceName = name,
                serviceType = info.first,
                totalSalesCount = info.second.first,
                totalRevenue = info.second.second,
                totalProfit = info.second.third
            )
        }.sortedByDescending { it.totalProfit }.take(5)

        // Recent Transactions Feed
        val feed = mutableListOf<RecentTransaction>()
        salesList.take(15).forEach {
            feed.add(
                RecentTransaction(
                    id = it.id,
                    type = "SALE",
                    title = "${it.serviceName} (${it.serviceType})",
                    subtitle = "Sold to ${it.customerName} • Qty ${it.quantity}",
                    amount = it.totalAmount,
                    profit = it.profit,
                    isIncome = true,
                    date = it.date,
                    paymentMethod = it.paymentMethod
                )
            )
        }
        expensesList.take(10).forEach {
            feed.add(
                RecentTransaction(
                    id = it.id,
                    type = "EXPENSE",
                    title = it.title,
                    subtitle = "Category: ${it.category}",
                    amount = it.amount,
                    profit = 0.0,
                    isIncome = false,
                    date = it.date,
                    paymentMethod = it.paymentMethod
                )
            )
        }
        purchasesList.take(10).forEach {
            feed.add(
                RecentTransaction(
                    id = it.id,
                    type = "PURCHASE",
                    title = "Purchase: ${it.serviceName}",
                    subtitle = "From ${it.supplierName} • Qty ${it.quantity}",
                    amount = it.totalAmount,
                    profit = 0.0,
                    isIncome = false,
                    date = it.date,
                    paymentMethod = it.paymentMethod
                )
            )
        }
        duePaymentsList.take(10).forEach {
            val isCust = it.partyType == "CUSTOMER"
            feed.add(
                RecentTransaction(
                    id = it.id,
                    type = if (isCust) "DUE_COLLECTION" else "DUE_PAYMENT",
                    title = if (isCust) "Due Received: ${it.partyName}" else "Due Paid: ${it.partyName}",
                    subtitle = it.note.ifBlank { "Ledger Settlement" },
                    amount = it.amount,
                    profit = 0.0,
                    isIncome = isCust,
                    date = it.date,
                    paymentMethod = it.paymentMethod
                )
            )
        }
        val sortedFeed = feed.sortedByDescending { it.date }.take(10)

        return DashboardSummary(
            todayIncome = todayIncome,
            todayExpense = todayExpense,
            todayProfit = todayProfit,
            totalCustomerDue = totalCustomerDue,
            totalSupplierDue = totalSupplierDue,
            totalSalesAmount = totalSalesAmount,
            totalNetProfit = totalNetProfit,
            monthlyChartData = monthlyPoints,
            topServices = topServices,
            recentTransactions = sortedFeed
        )
    }

    // Party Ledger Calculation
    fun getCustomerLedgerInfo(customerId: Long): PartyLedgerInfo {
        val cSales = sales.value.filter { it.customerId == customerId }
        val totalBilled = cSales.sumOf { it.totalAmount }
        val directSalesPaid = cSales.sumOf { it.paidAmount }
        val extraPayments = duePayments.value.filter { it.partyType == "CUSTOMER" && it.partyId == customerId }.sumOf { it.amount }
        val totalPaid = directSalesPaid + extraPayments
        val balanceDue = maxOf(0.0, totalBilled - totalPaid)
        return PartyLedgerInfo(totalBilled, totalPaid, balanceDue)
    }

    fun getSupplierLedgerInfo(supplierId: Long): PartyLedgerInfo {
        val sPurchases = purchases.value.filter { it.supplierId == supplierId }
        val totalBilled = sPurchases.sumOf { it.totalAmount }
        val directPurchasesPaid = sPurchases.sumOf { it.paidAmount }
        val extraPayments = duePayments.value.filter { it.partyType == "SUPPLIER" && it.partyId == supplierId }.sumOf { it.amount }
        val totalPaid = directPurchasesPaid + extraPayments
        val balanceDue = maxOf(0.0, totalBilled - totalPaid)
        return PartyLedgerInfo(totalBilled, totalPaid, balanceDue)
    }

    // PIN Authentication
    fun unlockWithPin(pin: String): Boolean {
        if (pin == settings.value.pinCode) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun lock() {
        if (settings.value.pinEnabled) {
            _isUnlocked.value = false
        }
    }

    // Action Methods
    fun saveService(service: ServiceEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveService(service)
            onDone()
        }
    }

    fun deleteService(service: ServiceEntity) {
        viewModelScope.launch {
            repository.deleteService(service)
        }
    }

    fun saveCustomer(customer: CustomerEntity, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveCustomer(customer)
            onDone(id)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun saveSupplier(supplier: SupplierEntity, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveSupplier(supplier)
            onDone(id)
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    fun recordSale(
        customerId: Long,
        customerName: String,
        service: ServiceEntity,
        quantity: Int,
        unitSellPrice: Double,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String,
        onDone: (SaleEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val sale = repository.recordSale(
                customerId,
                customerName,
                service,
                quantity,
                unitSellPrice,
                discount,
                paidAmount,
                paymentMethod,
                note
            )
            onDone(sale)
        }
    }

    fun deleteSale(sale: SaleEntity) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }

    fun recordPurchase(
        supplierId: Long,
        supplierName: String,
        serviceId: Long,
        serviceName: String,
        quantity: Int,
        unitBuyPrice: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordPurchase(
                supplierId,
                supplierName,
                serviceId,
                serviceName,
                quantity,
                unitBuyPrice,
                paidAmount,
                paymentMethod,
                note
            )
            onDone()
        }
    }

    fun deletePurchase(purchase: PurchaseEntity) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
        }
    }

    fun recordExpense(
        title: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        date: Long,
        note: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordExpense(title, category, amount, paymentMethod, date, note)
            onDone()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun recordDuePayment(
        partyType: String,
        partyId: Long,
        partyName: String,
        amount: Double,
        paymentMethod: String,
        note: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordDueTransaction(partyType, partyId, partyName, amount, paymentMethod, note)
            onDone()
        }
    }

    fun updateSettings(newSettings: BusinessSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun addServiceCategory(newCat: String) {
        val trimmed = newCat.trim()
        if (trimmed.isEmpty()) return
        val current = settings.value
        if (!current.serviceCategories.contains(trimmed)) {
            val updated = current.serviceCategories + trimmed
            updateSettings(current.copy(serviceCategories = updated))
        }
    }

    fun editServiceCategory(oldCat: String, newCat: String) {
        val trimmed = newCat.trim()
        if (trimmed.isEmpty() || trimmed == oldCat) return
        val current = settings.value
        val updatedList = current.serviceCategories.map { if (it == oldCat) trimmed else it }
        updateSettings(current.copy(serviceCategories = updatedList))

        // Update any existing services that use oldCat
        viewModelScope.launch {
            services.value.filter { it.category == oldCat }.forEach { s ->
                repository.saveService(s.copy(category = trimmed))
            }
        }
    }

    fun removeServiceCategory(cat: String) {
        val current = settings.value
        if (current.serviceCategories.size <= 1) return
        val updated = current.serviceCategories.filter { it != cat }
        updateSettings(current.copy(serviceCategories = updated))
    }

    fun addExpenseCategory(newCat: String) {
        val trimmed = newCat.trim()
        if (trimmed.isEmpty()) return
        val current = settings.value
        if (!current.expenseCategories.contains(trimmed)) {
            val updated = current.expenseCategories + trimmed
            updateSettings(current.copy(expenseCategories = updated))
        }
    }

    fun editExpenseCategory(oldCat: String, newCat: String) {
        val trimmed = newCat.trim()
        if (trimmed.isEmpty() || trimmed == oldCat) return
        val current = settings.value
        val updatedList = current.expenseCategories.map { if (it == oldCat) trimmed else it }
        updateSettings(current.copy(expenseCategories = updatedList))

        // Update any existing expenses that use oldCat
        viewModelScope.launch {
            expenses.value.filter { it.category == oldCat }.forEach { e ->
                repository.recordExpense(
                    title = e.title,
                    category = trimmed,
                    amount = e.amount,
                    paymentMethod = e.paymentMethod,
                    date = e.date,
                    note = e.note
                )
                repository.deleteExpense(e)
            }
        }
    }

    fun removeExpenseCategory(cat: String) {
        val current = settings.value
        if (current.expenseCategories.size <= 1) return
        val updated = current.expenseCategories.filter { it != cat }
        updateSettings(current.copy(expenseCategories = updated))
    }

    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun importBackupJson(
        jsonString: String,
        clearExisting: Boolean = false,
        onResult: (Result<BackupRestoreSummary>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val summary = repository.importBackupJson(jsonString, clearExisting)
                onResult(Result.success(summary))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun clearAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData()
            onDone()
        }
    }

    fun loadDemoData(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.loadDemoData()
            onDone(count)
        }
    }
}
