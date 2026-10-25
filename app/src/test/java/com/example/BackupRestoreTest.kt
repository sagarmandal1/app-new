package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.ServiceEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.model.BusinessSettings
import com.example.data.repository.BizRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BizRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BizRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test export backup contains all tables and verified test sale`() = runBlocking {
        // 1. Insert service
        val service = ServiceEntity(
            name = "Canva Pro License",
            type = "RESELL",
            category = "Software & Tools",
            costPrice = 450.0,
            sellPrice = 850.0,
            supplierName = "Cloud Wholesale"
        )
        val serviceId = repository.saveService(service)
        val savedService = service.copy(id = serviceId)

        // 2. Insert customer
        val customerId = repository.saveCustomer(
            CustomerEntity(
                name = "Rahim Chowdhury",
                phone = "01700112233",
                address = "Mirpur, Dhaka"
            )
        )

        // 3. Insert supplier
        val supplierId = repository.saveSupplier(
            SupplierEntity(
                name = "Cloud Wholesale",
                phone = "01900112233",
                company = "Cloud BD"
            )
        )

        // 4. Record a distinct test sale via repository
        val recordedSale = repository.recordSale(
            customerId = customerId,
            customerName = "Rahim Chowdhury",
            service = savedService,
            quantity = 3,
            unitSellPrice = 850.0,
            discount = 150.0,
            paidAmount = 2000.0,
            paymentMethod = "bKash",
            note = "Verification test sale for JSON backup"
        )
        val testInvoiceNumber = recordedSale.invoiceNumber

        // 5. Record purchase
        val recordedPurchase = repository.recordPurchase(
            supplierId = supplierId,
            supplierName = "Cloud Wholesale",
            serviceId = serviceId,
            serviceName = "Canva Pro License",
            quantity = 10,
            unitBuyPrice = 450.0,
            paidAmount = 4500.0,
            paymentMethod = "Bank Transfer",
            note = "Supplier purchase test"
        )

        // 6. Record expense
        repository.recordExpense(
            title = "Fiber Optical Broadband",
            category = "Internet & Utilities",
            amount = 1500.0,
            paymentMethod = "Nagad",
            date = System.currentTimeMillis(),
            note = "Office net bill"
        )

        // 7. Record due transaction
        repository.recordDueTransaction(
            partyType = "CUSTOMER",
            partyId = customerId,
            partyName = "Rahim Chowdhury",
            amount = 400.0,
            paymentMethod = "Cash",
            note = "Partial due collection"
        )

        // 8. Update Business Settings
        repository.updateSettings(
            BusinessSettings(
                businessName = "Apex Digital Services",
                ownerName = "Sagar",
                currency = "৳"
            )
        )

        // Export full JSON backup
        val jsonString = repository.exportBackupJson()

        // Verify test sale presence in JSON
        assertTrue("Test invoice number must be in backup JSON", jsonString.contains(testInvoiceNumber))
        assertTrue("Customer name must be in backup JSON", jsonString.contains("Rahim Chowdhury"))
        assertTrue("Service name must be in backup JSON", jsonString.contains("Canva Pro License"))
        assertTrue("Purchase number must be in backup JSON", jsonString.contains(recordedPurchase.purchaseNumber))
        assertTrue("Expense title must be in backup JSON", jsonString.contains("Fiber Optical Broadband"))
        assertTrue("Business name must be in backup JSON", jsonString.contains("Apex Digital Services"))

        // Verify JSON root has all 8 tables
        val jsonObject = JSONObject(jsonString)
        assertEquals("BizLedger", jsonObject.getString("app"))
        assertTrue(jsonObject.has("services"))
        assertTrue(jsonObject.has("customers"))
        assertTrue(jsonObject.has("suppliers"))
        assertTrue(jsonObject.has("sales"))
        assertTrue(jsonObject.has("purchases"))
        assertTrue(jsonObject.has("expenses"))
        assertTrue(jsonObject.has("due_payments"))
        assertTrue(jsonObject.has("app_config"))

        val salesArray = jsonObject.getJSONArray("sales")
        assertTrue("Sales array must have items", salesArray.length() >= 1)
        var foundSale = false
        for (i in 0 until salesArray.length()) {
            val s = salesArray.getJSONObject(i)
            if (s.getString("invoiceNumber") == testInvoiceNumber) {
                foundSale = true
                assertEquals("Rahim Chowdhury", s.getString("customerName"))
                assertEquals("Canva Pro License", s.getString("serviceName"))
                assertEquals(3, s.getInt("quantity"))
                assertEquals(850.0, s.getDouble("unitSellPrice"), 0.001)
                assertEquals(150.0, s.getDouble("discount"), 0.001)
                assertEquals(2400.0, s.getDouble("totalAmount"), 0.001)
                assertEquals(2000.0, s.getDouble("paidAmount"), 0.001)
                assertEquals(400.0, s.getDouble("dueAmount"), 0.001)
            }
        }
        assertTrue("Expected test sale was found in JSON", foundSale)

        // Test Restore
        // Clear database
        repository.clearAllData()
        assertEquals(0, db.saleDao().getAllSalesList().size)
        assertEquals(0, db.serviceDao().getAllServicesList().size)

        // Import backup back into database
        val restoreSummary = repository.importBackupJson(jsonString, clearExisting = true)
        assertTrue("Services restored", restoreSummary.servicesCount >= 1)
        assertTrue("Sales restored", restoreSummary.salesCount >= 1)
        assertTrue("Purchases restored", restoreSummary.purchasesCount >= 1)
        assertTrue("Expenses restored", restoreSummary.expensesCount >= 1)
        assertTrue("Customers restored", restoreSummary.customersCount >= 1)

        // Verify sale is restored in database
        val restoredSales = db.saleDao().getAllSalesList()
        val restoredSale = restoredSales.find { it.invoiceNumber == testInvoiceNumber }
        assertNotNull("Restored sale must exist in database", restoredSale)
        assertEquals("Rahim Chowdhury", restoredSale?.customerName)
        assertEquals(2400.0, restoredSale?.totalAmount ?: 0.0, 0.001)
        assertEquals(400.0, restoredSale?.dueAmount ?: 0.0, 0.001)
    }
}
