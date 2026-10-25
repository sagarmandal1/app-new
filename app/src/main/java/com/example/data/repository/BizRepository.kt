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
import com.example.data.model.BackupRestoreSummary
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
        root.put("app", "BizLedger")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Services
        val servicesList = db.serviceDao().getAllServicesList()
        val sArr = JSONArray()
        servicesList.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("type", s.type)
            obj.put("category", s.category)
            obj.put("costPrice", s.costPrice)
            obj.put("sellPrice", s.sellPrice)
            obj.put("supplierName", s.supplierName)
            obj.put("status", s.status)
            obj.put("description", s.description)
            obj.put("createdAt", s.createdAt)
            sArr.put(obj)
        }
        root.put("services", sArr)

        // 2. Customers
        val customersList = db.customerDao().getAllCustomersList()
        val cArr = JSONArray()
        customersList.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("email", c.email)
            obj.put("address", c.address)
            obj.put("notes", c.notes)
            obj.put("createdAt", c.createdAt)
            cArr.put(obj)
        }
        root.put("customers", cArr)

        // 3. Suppliers
        val suppliersList = db.supplierDao().getAllSuppliersList()
        val supArr = JSONArray()
        suppliersList.forEach { sup ->
            val obj = JSONObject()
            obj.put("id", sup.id)
            obj.put("name", sup.name)
            obj.put("phone", sup.phone)
            obj.put("email", sup.email)
            obj.put("company", sup.company)
            obj.put("address", sup.address)
            obj.put("notes", sup.notes)
            obj.put("createdAt", sup.createdAt)
            supArr.put(obj)
        }
        root.put("suppliers", supArr)

        // 4. Sales
        val salesList = db.saleDao().getAllSalesList()
        val slArr = JSONArray()
        salesList.forEach { sl ->
            val obj = JSONObject()
            obj.put("id", sl.id)
            obj.put("invoiceNumber", sl.invoiceNumber)
            obj.put("customerId", sl.customerId)
            obj.put("customerName", sl.customerName)
            obj.put("serviceId", sl.serviceId)
            obj.put("serviceName", sl.serviceName)
            obj.put("serviceType", sl.serviceType)
            obj.put("quantity", sl.quantity)
            obj.put("unitCostPrice", sl.unitCostPrice)
            obj.put("unitSellPrice", sl.unitSellPrice)
            obj.put("discount", sl.discount)
            obj.put("subtotal", sl.subtotal)
            obj.put("totalAmount", sl.totalAmount)
            obj.put("paidAmount", sl.paidAmount)
            obj.put("dueAmount", sl.dueAmount)
            obj.put("totalCost", sl.totalCost)
            obj.put("profit", sl.profit)
            obj.put("paymentMethod", sl.paymentMethod)
            obj.put("date", sl.date)
            obj.put("note", sl.note)
            slArr.put(obj)
        }
        root.put("sales", slArr)

        // 5. Purchases
        val purchasesList = db.purchaseDao().getAllPurchasesList()
        val pArr = JSONArray()
        purchasesList.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("purchaseNumber", p.purchaseNumber)
            obj.put("supplierId", p.supplierId)
            obj.put("supplierName", p.supplierName)
            obj.put("serviceId", p.serviceId)
            obj.put("serviceName", p.serviceName)
            obj.put("quantity", p.quantity)
            obj.put("unitBuyPrice", p.unitBuyPrice)
            obj.put("totalAmount", p.totalAmount)
            obj.put("paidAmount", p.paidAmount)
            obj.put("dueAmount", p.dueAmount)
            obj.put("paymentMethod", p.paymentMethod)
            obj.put("date", p.date)
            obj.put("note", p.note)
            pArr.put(obj)
        }
        root.put("purchases", pArr)

        // 6. Expenses
        val expensesList = db.expenseDao().getAllExpensesList()
        val eArr = JSONArray()
        expensesList.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("category", e.category)
            obj.put("amount", e.amount)
            obj.put("paymentMethod", e.paymentMethod)
            obj.put("date", e.date)
            obj.put("note", e.note)
            eArr.put(obj)
        }
        root.put("expenses", eArr)

        // 7. Due Payments
        val duePaymentsList = db.duePaymentDao().getAllDuePaymentsList()
        val dpArr = JSONArray()
        duePaymentsList.forEach { dp ->
            val obj = JSONObject()
            obj.put("id", dp.id)
            obj.put("partyType", dp.partyType)
            obj.put("partyId", dp.partyId)
            obj.put("partyName", dp.partyName)
            obj.put("amount", dp.amount)
            obj.put("paymentMethod", dp.paymentMethod)
            obj.put("date", dp.date)
            obj.put("note", dp.note)
            dpArr.put(obj)
        }
        root.put("due_payments", dpArr)

        // 8. Settings / App Config
        val configsList = db.configDao().getAllConfigsList()
        val cfgObj = JSONObject()
        configsList.forEach { cfg ->
            cfgObj.put(cfg.key, cfg.value)
        }
        root.put("app_config", cfgObj)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String, clearExisting: Boolean = false): BackupRestoreSummary {
        val root = JSONObject(jsonString)
        if (clearExisting) {
            clearAllData()
        }

        // Parse services
        val sArr = root.optJSONArray("services") ?: JSONArray()
        val services = mutableListOf<ServiceEntity>()
        for (i in 0 until sArr.length()) {
            val obj = sArr.getJSONObject(i)
            services.add(
                ServiceEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    type = obj.optString("type", "RESELL"),
                    category = obj.optString("category", "General"),
                    costPrice = obj.optDouble("costPrice", 0.0),
                    sellPrice = obj.optDouble("sellPrice", 0.0),
                    supplierName = obj.optString("supplierName", ""),
                    status = obj.optString("status", "ACTIVE"),
                    description = obj.optString("description", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse customers
        val cArr = root.optJSONArray("customers") ?: JSONArray()
        val customers = mutableListOf<CustomerEntity>()
        for (i in 0 until cArr.length()) {
            val obj = cArr.getJSONObject(i)
            customers.add(
                CustomerEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    address = obj.optString("address", ""),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse suppliers
        val supArr = root.optJSONArray("suppliers") ?: JSONArray()
        val suppliers = mutableListOf<SupplierEntity>()
        for (i in 0 until supArr.length()) {
            val obj = supArr.getJSONObject(i)
            suppliers.add(
                SupplierEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    company = obj.optString("company", ""),
                    address = obj.optString("address", ""),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse sales
        val slArr = root.optJSONArray("sales") ?: JSONArray()
        val sales = mutableListOf<SaleEntity>()
        for (i in 0 until slArr.length()) {
            val obj = slArr.getJSONObject(i)
            sales.add(
                SaleEntity(
                    id = obj.optLong("id", 0L),
                    invoiceNumber = obj.getString("invoiceNumber"),
                    customerId = obj.optLong("customerId", 0L),
                    customerName = obj.optString("customerName", "Customer"),
                    serviceId = obj.optLong("serviceId", 0L),
                    serviceName = obj.optString("serviceName", "Service"),
                    serviceType = obj.optString("serviceType", "RESELL"),
                    quantity = obj.optInt("quantity", 1),
                    unitCostPrice = obj.optDouble("unitCostPrice", 0.0),
                    unitSellPrice = obj.optDouble("unitSellPrice", 0.0),
                    discount = obj.optDouble("discount", 0.0),
                    subtotal = obj.optDouble("subtotal", 0.0),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    dueAmount = obj.optDouble("dueAmount", 0.0),
                    totalCost = obj.optDouble("totalCost", 0.0),
                    profit = obj.optDouble("profit", 0.0),
                    paymentMethod = obj.optString("paymentMethod", "Cash"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = obj.optString("note", "")
                )
            )
        }

        // Parse purchases
        val pArr = root.optJSONArray("purchases") ?: JSONArray()
        val purchases = mutableListOf<PurchaseEntity>()
        for (i in 0 until pArr.length()) {
            val obj = pArr.getJSONObject(i)
            purchases.add(
                PurchaseEntity(
                    id = obj.optLong("id", 0L),
                    purchaseNumber = obj.getString("purchaseNumber"),
                    supplierId = obj.optLong("supplierId", 0L),
                    supplierName = obj.optString("supplierName", "Supplier"),
                    serviceId = obj.optLong("serviceId", 0L),
                    serviceName = obj.optString("serviceName", "Service"),
                    quantity = obj.optInt("quantity", 1),
                    unitBuyPrice = obj.optDouble("unitBuyPrice", 0.0),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    dueAmount = obj.optDouble("dueAmount", 0.0),
                    paymentMethod = obj.optString("paymentMethod", "Cash"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = obj.optString("note", "")
                )
            )
        }

        // Parse expenses
        val eArr = root.optJSONArray("expenses") ?: JSONArray()
        val expenses = mutableListOf<ExpenseEntity>()
        for (i in 0 until eArr.length()) {
            val obj = eArr.getJSONObject(i)
            expenses.add(
                ExpenseEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    category = obj.optString("category", "General"),
                    amount = obj.optDouble("amount", 0.0),
                    paymentMethod = obj.optString("paymentMethod", "Cash"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = obj.optString("note", "")
                )
            )
        }

        // Parse due payments
        val dpArr = root.optJSONArray("due_payments") ?: JSONArray()
        val duePayments = mutableListOf<DuePaymentEntity>()
        for (i in 0 until dpArr.length()) {
            val obj = dpArr.getJSONObject(i)
            duePayments.add(
                DuePaymentEntity(
                    id = obj.optLong("id", 0L),
                    partyType = obj.getString("partyType"),
                    partyId = obj.optLong("partyId", 0L),
                    partyName = obj.optString("partyName", ""),
                    amount = obj.optDouble("amount", 0.0),
                    paymentMethod = obj.optString("paymentMethod", "Cash"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = obj.optString("note", "")
                )
            )
        }

        // Parse app_config
        val cfgObj = root.optJSONObject("app_config")
        if (cfgObj != null) {
            val configs = mutableListOf<AppConfigEntity>()
            val keys = cfgObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                configs.add(AppConfigEntity(k, cfgObj.getString(k)))
            }
            if (configs.isNotEmpty()) {
                db.configDao().insertConfigs(configs)
            }
        }

        // Restore tables with REPLACE conflict strategy
        if (services.isNotEmpty()) db.serviceDao().insertAll(services)
        if (customers.isNotEmpty()) db.customerDao().insertAll(customers)
        if (suppliers.isNotEmpty()) db.supplierDao().insertAll(suppliers)
        if (sales.isNotEmpty()) db.saleDao().insertAll(sales)
        if (purchases.isNotEmpty()) db.purchaseDao().insertAll(purchases)
        if (expenses.isNotEmpty()) db.expenseDao().insertAll(expenses)
        if (duePayments.isNotEmpty()) db.duePaymentDao().insertAll(duePayments)

        return BackupRestoreSummary(
            servicesCount = services.size,
            customersCount = customers.size,
            suppliersCount = suppliers.size,
            salesCount = sales.size,
            purchasesCount = purchases.size,
            expensesCount = expenses.size,
            duePaymentsCount = duePayments.size
        )
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

        // Seed default config only (categories & payment methods, blank business info)
        updateSettings(BusinessSettings())
        db.configDao().setConfig(AppConfigEntity("is_seeded", "true"))
    }

    /**
     * Optional demo data loader triggered only from Settings for testing/evaluation.
     */
    suspend fun loadDemoData(): Int {
        var count = 0

        // Starter services (Resell & Own)
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
                costPrice = 1200.0,
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
                costPrice = 600.0,
                sellPrice = 3500.0,
                supplierName = "",
                status = "ACTIVE",
                description = "Logo, typography, banner pack and 10 Instagram post templates"
            )
        )
        db.serviceDao().insertAll(starterServices)
        count += starterServices.size

        // Starter customers
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
        count += starterCustomers.size

        // Starter suppliers
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
        count += starterSuppliers.size

        // Sample sale
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
            profit = 700.0,
            paymentMethod = "bKash",
            date = System.currentTimeMillis() - 86400000L,
            note = "Client paid partial via bKash, 400 BDT due next week"
        )
        db.saleDao().insertSale(initialSale)
        count += 1

        // Sample expense
        val initialExpense = ExpenseEntity(
            title = "Office High-Speed Optical Fiber Internet",
            category = "Internet & Utilities",
            amount = 1200.0,
            paymentMethod = "Nagad",
            date = System.currentTimeMillis() - 43200000L,
            note = "Monthly broadband bill"
        )
        db.expenseDao().insertExpense(initialExpense)
        count += 1

        return count
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
