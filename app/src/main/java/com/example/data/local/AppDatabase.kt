package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ConfigDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.DuePaymentDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.PurchaseDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.ServiceDao
import com.example.data.local.dao.SupplierDao
import com.example.data.local.entity.AppConfigEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DuePaymentEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.ServiceEntity
import com.example.data.local.entity.SupplierEntity

@Database(
    entities = [
        ServiceEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleEntity::class,
        PurchaseEntity::class,
        ExpenseEntity::class,
        DuePaymentEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun duePaymentDao(): DuePaymentDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biz_ledger_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
