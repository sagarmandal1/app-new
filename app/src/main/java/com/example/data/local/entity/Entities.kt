package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "RESELL" or "OWN"
    val category: String,
    val costPrice: Double, // Buy price for Resell, Making/Production cost for Own
    val sellPrice: Double,
    val supplierName: String = "",
    val status: String = "ACTIVE", // "ACTIVE" or "INACTIVE"
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val profitPerUnit: Double
        get() = sellPrice - costPrice

    val marginPercent: Double
        get() = if (sellPrice > 0) ((sellPrice - costPrice) / sellPrice) * 100 else 0.0
}

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long,
    val customerName: String,
    val serviceId: Long,
    val serviceName: String,
    val serviceType: String, // "RESELL" or "OWN"
    val quantity: Int,
    val unitCostPrice: Double,
    val unitSellPrice: Double,
    val discount: Double,
    val subtotal: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val totalCost: Double,
    val profit: Double,
    val paymentMethod: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseNumber: String,
    val supplierId: Long,
    val supplierName: String,
    val serviceId: Long,
    val serviceName: String,
    val quantity: Int,
    val unitBuyPrice: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentMethod: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "due_payments")
data class DuePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partyType: String, // "CUSTOMER" or "SUPPLIER"
    val partyId: Long,
    val partyName: String,
    val amount: Double,
    val paymentMethod: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
