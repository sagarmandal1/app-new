package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppConfigEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DuePaymentEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.ServiceEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.model.BusinessSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BizRepository(private val db: AppDatabase) {

    val services: Flow<List<ServiceEntity>> = db.serviceDao().getAllServices()
    val activeServices: Flow<List<ServiceEntity>> = db.serviceDao().getActiveServices()
    val customers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val suppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
    val sales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val purchases: Flow<List<PurchaseEntity>> = db.purchaseDao().getAllPurchases()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    val duePayments: Flow<List<DuePaymentEntity>> = db.duePaymentDao().getAllDuePayments()

    val settings: Flow<BusinessSettings> = db.configDao().getAllConfigs().map { configs ->
        val map = configs.associate { it.key to it.value }
        val default = BusinessSettings()

        val serviceCats = map["service_categories"]?.let { parseStringList(it) } ?: default.serviceCategories
        val expenseCats = map["expense_categories"]?.let { parseStringList(it) } ?: default.expenseCategories
        val payMethods = map["payment_methods"]?.let { parseStringList(it) } ?: default.paymentMethods

        BusinessSettings(
            businessName = map["business_name"] ?: default.businessName,
            ownerName = map["owner_name"] ?: default.ownerName,
            phone = map["phone"] ?: default.phone,
            email = map["email"] ?: default.email,
            address = map["address"] ?: default.address,
            currency = map["currency"] ?: default.currency,
            language = map["language"] ?: default.language,
            pinEnabled = map["pin_enabled"]?.toBooleanStrictOrNull() ?: default.pinEnabled,
            pinCode = map["pin_code"] ?: default.pinCode,
            serviceCategories = serviceCats,
            expenseCategories = expenseCats,
            paymentMethods = payMethods,
            isDarkMode = map["is_dark_mode"]?.toBooleanStrictOrNull() ?: default.isDarkMode
        )
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            checkAndSeedInitialData()
        }
    }

    // Services
    suspend fun saveService(service: ServiceEntity): Long {
        return if (service.id == 0L) {
            db.serviceDao().insertService(service)
        } else {
            db.serviceDao().updateService(service)
            service.id
        }
    }

    suspend fun deleteService(service: ServiceEntity) {
        db.serviceDao().deleteService(service)
    }

    // Customers
    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            db.customerDao().insertCustomer(customer)
        } else {
            db.customerDao().updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        db.customerDao().deleteCustomer(customer)
    }

    // Suppliers
    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        return if (supplier.id == 0L) {
            db.supplierDao().insertSupplier(supplier)
        } else {
            db.supplierDao().updateSupplier(supplier)
            supplier.id
        }
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) {
        db.supplierDao().deleteSupplier(supplier)
    }

    // Sales
    suspend fun recordSale(
        customerId: Long,
        customerName: String,
        service: ServiceEntity,
        quantity: Int,
        unitSellPrice: Double,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ): SaleEntity {
        val invoiceNo = "INV-" + (System.currentTimeMillis() % 1000000)
        val unitCost = service.costPrice
        val subtotal = quantity * unitSellPrice
        val total = maxOf(0.0, subtotal - discount)
        val actualPaid = minOf(total, maxOf(0.0, paidAmount))
        val due = maxOf(0.0, total - actualPaid)
        val totalCost = quantity * unitCost
        val profit = total - totalCost

        val sale = SaleEntity(
            invoiceNumber = invoiceNo,
            customerId = customerId,
            customerName = customerName,
            serviceId = service.id,
            serviceName = service.name,
            serviceType = service.type,
            quantity = quantity,
            unitCostPrice = unitCost,
            unitSellPrice = unitSellPrice,
            discount = discount,
            subtotal = subtotal,
            totalAmount = total,
            paidAmount = actualPaid,
            dueAmount = due,
            totalCost = totalCost,
            profit = profit,
            paymentMethod = paymentMethod,
            date = System.currentTimeMillis(),
            note = note
        )
        val id = db.saleDao().insertSale(sale)
        return sale.copy(id = id)
    }

    suspend fun deleteSale(sale: SaleEntity) {
        db.saleDao().deleteSale(sale)
    }

    // Purchases
    suspend fun recordPurchase(
        supplierId: Long,
        supplierName: String,
        serviceId: Long,
        serviceName: String,
        quantity: Int,
        unitBuyPrice: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ): PurchaseEntity {
        val purNo = "PUR-" + (System.currentTimeMillis() % 1000000)
        val total = quantity * unitBuyPrice
        val actualPaid = minOf(total, maxOf(0.0, paidAmount))
        val due = maxOf(0.0, total - actualPaid)

        val purchase = PurchaseEntity(
            purchaseNumber = purNo,
            supplierId = supplierId,
            supplierName = supplierName,
            serviceId = serviceId,
            serviceName = serviceName,
            quantity = quantity,
            unitBuyPrice = unitBuyPrice,
            totalAmount = total,
            paidAmount = actualPaid,
            dueAmount = due,
            paymentMethod = paymentMethod,
            date = System.currentTimeMillis(),
            note = note
        )
        val id = db.purchaseDao().insertPurchase(purchase)
        return purchase.copy(id = id)
    }

    suspend fun deletePurchase(purchase: PurchaseEntity) {
        db.purchaseDao().deletePurchase(purchase)
    }

    // Expenses
    suspend fun recordExpense(
        title: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        date: Long,
        note: String
    ): Long {
        val expense = ExpenseEntity(
            title = title,
            category = category,
            amount = amount,
            paymentMethod = paymentMethod,
            date = date,
            note = note
        )
        return db.expenseDao().insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        db.expenseDao().deleteExpense(expense)
    }

    // Due Payment / Collection
    suspend fun recordDueTransaction(
        partyType: String,
        partyId: Long,
        partyName: String,
        amount: Double,
        paymentMethod: String,
        note: String
    ): Long {
        val record = DuePaymentEntity(
            partyType = partyType,
            partyId = partyId,
            partyName = partyName,
            amount = amount,
            paymentMethod = paymentMethod,
            date = System.currentTimeMillis(),
            note = note
        )
        return db.duePaymentDao().insertDuePayment(record)
    }

    suspend fun deleteDuePayment(payment: DuePaymentEntity) {
        db.duePaymentDao().deleteDuePayment(payment)
    }

    // Settings Updates
    suspend fun updateSetting(key: String, value: String) {
        db.configDao().setConfig(AppConfigEntity(key, value))
    }

    suspend fun updateSettings(settings: BusinessSettings) {
        db.configDao().insertConfigs(
            listOf(
                AppConfigEntity("business_name", settings.businessName),
                AppConfigEntity("owner_name", settings.ownerName),
                AppConfigEntity("phone", settings.phone),
                AppConfigEntity("email", settings.email),
                AppConfigEntity("address", settings.address),
                AppConfigEntity("currency", settings.currency),
                AppConfigEntity("language", settings.language),
                AppConfigEntity("pin_enabled", settings.pinEnabled.toString()),
                AppConfigEntity("pin_code", settings.pinCode),
                AppConfigEntity("service_categories", serializeStringList(settings.serviceCategories)),
                AppConfigEntity("expense_categories", serializeStringList(settings.expenseCategories)),
                AppConfigEntity("payment_methods", serializeStringList(settings.paymentMethods)),
                AppConfigEntity("is_dark_mode", settings.isDarkMode.toString())
            )
        )
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        val sList = JSONArray()
        val cList = JSONArray()
        val supList = JSONArray()
        val slList = JSONArray()
        val pList = JSONArray()
        val eList = JSONArray()

        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("services", sList)
        root.put("customers", cList)
        root.put("suppliers", supList)
        root.put("sales", slList)
        root.put("purchases", pList)
        root.put("expenses", eList)
        return root.toString(2)
    }

    suspend fun clearAllData() {
        db.saleDao().clearAll()
        db.purchaseDao().clearAll()
        db.expenseDao().clearAll()
        db.duePaymentDao().clearAll()
        db.customerDao().clearAll()
        db.supplierDao().clearAll()
        db.serviceDao().clearAll()
    }

    private suspend fun checkAndSeedInitialData() {
        val existing = db.configDao().getConfig("is_seeded")
        if (existing == "true") return

        // Seed default config
        updateSettings(BusinessSettings())
        db.configDao().setConfig(AppConfigEntity("is_seeded", "true"))

        // Seed starter services (Resell & Own)
        val starterServices = listOf(
            ServiceEntity(
                name = "Canva Pro Annual License",
                type = "RESELL",
                category = "Subscription & OTT",
                costPrice = 450.0,
                sellPrice = 850.0,
                supplierName = "Global Digital Tech",
                status = "ACTIVE",
                description = "Annual Canva Pro EDU/Team invitation license"
            ),
            ServiceEntity(
                name = "Custom Landing Page Design",
                type = "OWN",
                category = "Web & App Development",
                costPrice = 1200.0, // Domain/Hosting/Design asset cost
                sellPrice = 5500.0,
                supplierName = "",
                status = "ACTIVE",
                description = "Custom responsive single page website built in Next.js/Tailwind"
            ),
            ServiceEntity(
                name = "Google Workspace 1-Year Seat",
                type = "RESELL",
                category = "Software & Tools",
                costPrice = 3200.0,
                sellPrice = 4800.0,
                supplierName = "Cloud Reseller BD",
                status = "ACTIVE",
                description = "Official business email and cloud storage"
            ),
            ServiceEntity(
                name = "Social Media Brand Identity Kit",
                type = "OWN",
                category = "Graphic & UI Design",
                costPrice = 600.0, // Fonts and stock graphic resources
                sellPrice = 3500.0,
                supplierName = "",
                status = "ACTIVE",
                description = "Logo, typography, banner pack and 10 Instagram post templates"
            )
        )
        db.serviceDao().insertAll(starterServices)

        // Seed initial customers
        val starterCustomers = listOf(
            CustomerEntity(
                name = "Tanvir Ahmed",
                phone = "01711223344",
                email = "tanvir@techspark.io",
                address = "Dhanmondi, Dhaka",
                notes = "Regular software client"
            ),
            CustomerEntity(
                name = "Nusrat Jahan",
                phone = "01822334455",
                email = "nusrat@creativebd.com",
                address = "Uttara Sector 7, Dhaka",
                notes = "Design and marketing projects"
            )
        )
        db.customerDao().insertAll(starterCustomers)

        // Seed initial suppliers
        val starterSuppliers = listOf(
            SupplierEntity(
                name = "Global Digital Tech",
                phone = "01933445566",
                email = "sales@globaldigitaltech.com",
                company = "Global Digital Wholesale Ltd",
                address = "Motijheel, Dhaka",
                notes = "Software license & API distributor"
            ),
            SupplierEntity(
                name = "Cloud Reseller BD",
                phone = "01644556677",
                email = "support@cloudreseller.bd",
                company = "Cloud Networks BD",
                address = "Banani, Dhaka",
                notes = "Google and Microsoft workspace distributor"
            )
        )
        db.supplierDao().insertAll(starterSuppliers)

        // Seed sample sale
        val initialSale = SaleEntity(
            invoiceNumber = "INV-10024",
            customerId = 1,
            customerName = "Tanvir Ahmed",
            serviceId = 1,
            serviceName = "Canva Pro Annual License",
            serviceType = "RESELL",
            quantity = 2,
            unitCostPrice = 450.0,
            unitSellPrice = 850.0,
            discount = 100.0,
            subtotal = 1700.0,
            totalAmount = 1600.0,
            paidAmount = 1200.0,
            dueAmount = 400.0,
            totalCost = 900.0,
            profit = 700.0, // 1600 - 900
            paymentMethod = "bKash",
            date = System.currentTimeMillis() - 86400000L,
            note = "Client paid partial via bKash, 400 BDT due next week"
        )
        db.saleDao().insertSale(initialSale)

        // Seed sample expense
        val initialExpense = ExpenseEntity(
            title = "Office High-Speed Optical Fiber Internet",
            category = "Internet & Utilities",
            amount = 1200.0,
            paymentMethod = "Nagad",
            date = System.currentTimeMillis() - 43200000L,
            note = "Monthly broadband bill"
        )
        db.expenseDao().insertExpense(initialExpense)
    }

    private fun serializeStringList(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun parseStringList(json: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        return if (list.isNotEmpty()) list else BusinessSettings().serviceCategories
    }
}
