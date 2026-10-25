package com.example.data.model

data class BusinessSettings(
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val currency: String = "৳",
    val language: String = "en", // "en" or "bn"
    val pinEnabled: Boolean = false,
    val pinCode: String = "",
    val biometricEnabled: Boolean = true,
    val serviceCategories: List<String> = listOf(
        "Software & Tools",
        "Subscription & OTT",
        "Digital Marketing",
        "Graphic & UI Design",
        "Web & App Development",
        "Hosting & Domain",
        "Consulting",
        "General Services"
    ),
    val expenseCategories: List<String> = listOf(
        "Internet & Utilities",
        "Ads & Marketing",
        "Software Subscriptions",
        "Transport & Travel",
        "Office Rent",
        "Salary & Labor",
        "Equipment & Tools",
        "Other Expenses"
    ),
    val paymentMethods: List<String> = listOf(
        "Cash",
        "bKash",
        "Nagad",
        "Bank Transfer",
        "Rocket",
        "Due / Credit"
    ),
    val isDarkMode: Boolean = false
)

data class BackupRestoreSummary(
    val servicesCount: Int = 0,
    val customersCount: Int = 0,
    val suppliersCount: Int = 0,
    val salesCount: Int = 0,
    val purchasesCount: Int = 0,
    val expensesCount: Int = 0,
    val duePaymentsCount: Int = 0
) {
    val totalRecords: Int
        get() = servicesCount + customersCount + suppliersCount + salesCount + purchasesCount + expensesCount + duePaymentsCount
}

object AppStrings {
    fun get(key: String, lang: String): String {
        val isBn = lang == "bn"
        return when (key) {
            // Navigation
            "nav_dashboard" -> if (isBn) "ড্যাশবোর্ড" else "Dashboard"
            "nav_sales" -> if (isBn) "বিক্রয়" else "Sales"
            "nav_services" -> if (isBn) "সেবা সমূহ" else "Services"
            "nav_ledger" -> if (isBn) "খতিয়ান" else "Ledger"
            "nav_reports" -> if (isBn) "রিপোর্টস" else "Reports"
            "nav_settings" -> if (isBn) "সেটিংস" else "Settings"

            // Dashboard
            "today_income" -> if (isBn) "আজকের আয়" else "Today's Income"
            "today_expense" -> if (isBn) "আজকের খরচ" else "Today's Expense"
            "today_profit" -> if (isBn) "আজকের লাভ" else "Today's Profit"
            "due_to_collect" -> if (isBn) "মোট পাওনা (বাকি)" else "Due to Collect"
            "due_to_pay" -> if (isBn) "মোট দেনা (সরবরাহকারী)" else "Due to Pay"
            "quick_actions" -> if (isBn) "দ্রুত কার্যক্রম" else "Quick Actions"
            "add_sale" -> if (isBn) "বিক্রয় যোগ করুন" else "Add Sale"
            "add_purchase" -> if (isBn) "ক্রয় যোগ করুন" else "Add Purchase"
            "add_expense" -> if (isBn) "খরচ যোগ করুন" else "Add Expense"
            "income_vs_expense" -> if (isBn) "আয় বনাম খরচ বিশ্লেষণ" else "Income vs Expense"
            "profit_trend" -> if (isBn) "মুনাফার গতিবিধি" else "Profit Trend"
            "top_services" -> if (isBn) "শীর্ষ লাভজনক সেবা" else "Top Performing Services"
            "recent_transactions" -> if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Transactions"
            "view_all" -> if (isBn) "সব দেখুন" else "View All"
            "no_transactions" -> if (isBn) "এখনও কোন লেনদেন হয়নি" else "No transactions yet"

            // Services
            "services_title" -> if (isBn) "সেবা ব্যবস্থাপনা" else "Services Management"
            "all_services" -> if (isBn) "সকল সেবা" else "All Services"
            "resell_service" -> if (isBn) "রিসেল সার্ভিস" else "Resell Service"
            "own_service" -> if (isBn) "নিজস্ব সার্ভিস" else "Own Service"
            "buy_price" -> if (isBn) "ক্রয় মূল্য" else "Buy Price"
            "making_cost" -> if (isBn) "তৈরি/উৎপাদন খরচ" else "Production Cost"
            "sell_price" -> if (isBn) "বিক্রয় মূল্য" else "Sell Price"
            "profit_unit" -> if (isBn) "একক প্রতি লাভ" else "Profit / Unit"
            "margin" -> if (isBn) "মার্জিন" else "Margin"
            "supplier" -> if (isBn) "সরবরাহকারী" else "Supplier"
            "category" -> if (isBn) "ক্যাটাগরি" else "Category"
            "status_active" -> if (isBn) "সক্রিয়" else "Active"
            "status_inactive" -> if (isBn) "নিষ্ক্রিয়" else "Inactive"
            "add_new_service" -> if (isBn) "নতুন সেবা যুক্ত করুন" else "Add New Service"
            "edit_service" -> if (isBn) "সেবা সম্পাদনা" else "Edit Service"
            "service_name" -> if (isBn) "সেবার নাম" else "Service Name"
            "service_desc" -> if (isBn) "বিবরণ (ঐচ্ছিক)" else "Description (Optional)"

            // Sales
            "sales_title" -> if (isBn) "বিক্রয় রেজিস্টার" else "Sales Register"
            "new_sale" -> if (isBn) "নতুন বিক্রয় তৈরি করুন" else "Create New Sale"
            "customer" -> if (isBn) "গ্রাহক" else "Customer"
            "select_customer" -> if (isBn) "গ্রাহক নির্বাচন করুন" else "Select Customer"
            "select_service" -> if (isBn) "সেবা নির্বাচন করুন" else "Select Service"
            "quantity" -> if (isBn) "পরিমাণ" else "Quantity"
            "discount" -> if (isBn) "ছাড় / ডিসকাউন্ট" else "Discount"
            "subtotal" -> if (isBn) "উপমোট" else "Subtotal"
            "total_amount" -> if (isBn) "মোট প্রদেয়" else "Total Amount"
            "paid_amount" -> if (isBn) "পরিশোধিত টাকা" else "Paid Amount"
            "due_remaining" -> if (isBn) "অবশিষ্ট বাকি" else "Remaining Due"
            "payment_method" -> if (isBn) "পেমেন্ট মাধ্যম" else "Payment Method"
            "sale_profit" -> if (isBn) "এই বিক্রয়ে লাভ" else "Profit on Sale"
            "view_invoice" -> if (isBn) "ইনভয়েস / বিল" else "View Invoice"
            "share_bill" -> if (isBn) "ইনভয়েস শেয়ার করুন" else "Share Invoice"

            // Purchases
            "purchases_title" -> if (isBn) "ক্রয় ব্যবস্থাপনা (সরবরাহকারী)" else "Purchases Register"
            "new_purchase" -> if (isBn) "নতুন ক্রয় লিপিবদ্ধ করুন" else "Record Purchase"

            // Expenses
            "expense_title" -> if (isBn) "খরচ ব্যবস্থাপনা" else "Expense Register"
            "expense_amount" -> if (isBn) "খরচের পরিমাণ" else "Expense Amount"
            "expense_note" -> if (isBn) "নোট / বিবরণ" else "Note / Description"

            // Ledger
            "ledger_title" -> if (isBn) "খতিয়ান ও বাকি হিসাব" else "Customers & Suppliers Ledger"
            "customers_tab" -> if (isBn) "গ্রাহকবৃন্দ" else "Customers"
            "suppliers_tab" -> if (isBn) "সরবরাহকারীগণ" else "Suppliers"
            "total_bought" -> if (isBn) "মোট ক্রয়/অর্ডার" else "Total Ordered"
            "total_paid" -> if (isBn) "মোট পরিশোধ" else "Total Paid"
            "current_due" -> if (isBn) "বর্তমান বাকি" else "Current Due"
            "collect_due" -> if (isBn) "বাকি আদায়" else "Collect Due"
            "pay_due" -> if (isBn) "বাকি পরিশোধ" else "Pay Supplier"
            "add_customer" -> if (isBn) "নতুন গ্রাহক" else "Add Customer"
            "add_supplier" -> if (isBn) "নতুন সরবরাহকারী" else "Add Supplier"
            "send_reminder" -> if (isBn) "তাকিদ পাঠান" else "Send Reminder"

            // Reports
            "reports_title" -> if (isBn) "হিসাব ও মুনাফা রিপোর্ট" else "Reports & Analytics"
            "filter_all" -> if (isBn) "সর্বমোট" else "All Time"
            "filter_today" -> if (isBn) "আজ" else "Today"
            "filter_week" -> if (isBn) "এই সপ্তাহ" else "This Week"
            "filter_month" -> if (isBn) "এই মাস" else "This Month"
            "filter_year" -> if (isBn) "এই বছর" else "This Year"
            "total_sales" -> if (isBn) "মোট বিক্রয়" else "Total Sales"
            "net_profit" -> if (isBn) "নিট লাভ (লাভ - খরচ)" else "Net Profit"
            "total_expenses" -> if (isBn) "মোট খরচ" else "Total Expenses"
            "export_csv" -> if (isBn) "CSV এক্সপোর্ট" else "Export CSV"
            "export_pdf" -> if (isBn) "রিপোর্ট শেয়ার" else "Share Report"

            // Settings
            "business_info" -> if (isBn) "ব্যবসার তথ্য ও প্রোফাইল" else "Business Info & Profile"
            "currency" -> if (isBn) "মুদ্রা প্রতীক" else "Currency Symbol"
            "language" -> if (isBn) "ভাষা / Language" else "Language"
            "pin_security" -> if (isBn) "পিন সুরক্ষা লক" else "PIN Security Lock"
            "manage_categories" -> if (isBn) "ক্যাটাগরি ব্যবস্থাপনা" else "Manage Categories"
            "manage_payment_methods" -> if (isBn) "পেমেন্ট মাধ্যম ব্যবস্থাপনা" else "Payment Methods"
            "backup_restore" -> if (isBn) "ডাটা ব্যাকআপ ও রিস্টোর" else "Backup & Restore"
            "export_backup" -> if (isBn) "সম্পূর্ণ ব্যাকআপ তৈরি (JSON)" else "Export Full Backup (JSON)"
            "import_backup" -> if (isBn) "ব্যাকআপ থেকে রিস্টোর" else "Import / Restore Backup"
            "clear_data" -> if (isBn) "সব ডাটা মুছুন" else "Reset / Clear Data"
            "export_success" -> if (isBn) "ব্যাকআপ ফাইল সফলভাবে তৈরি হয়েছে!" else "Backup exported successfully!"
            "restore_success" -> if (isBn) "ডাটা সফলভাবে রিস্টোর হয়েছে!" else "Data restored successfully!"
            "restore_error" -> if (isBn) "রিস্টোর ব্যর্থ হয়েছে! সঠিক JSON দিন।" else "Restore failed! Please provide valid JSON."
            "paste_backup_json" -> if (isBn) "ব্যাকআপ JSON কোড পেস্ট করুন" else "Paste Backup JSON"

            // Common
            "save" -> if (isBn) "সংরক্ষণ করুন" else "Save"
            "cancel" -> if (isBn) "বাতিল" else "Cancel"
            "delete" -> if (isBn) "মুছুন" else "Delete"
            "edit" -> if (isBn) "সম্পাদনা" else "Edit"
            "search" -> if (isBn) "অনুসন্ধান..." else "Search..."
            "confirm" -> if (isBn) "নিশ্চিত করুন" else "Confirm"
            "success" -> if (isBn) "সফল হয়েছে!" else "Success!"
            else -> key
        }
    }
}
