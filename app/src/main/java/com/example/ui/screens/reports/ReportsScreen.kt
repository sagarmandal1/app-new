package com.example.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.model.AppStrings
import com.example.ui.components.FormatCurrency
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SecondaryTeal
import com.example.ui.viewmodel.BizViewModel
import java.util.Calendar

@Composable
fun ReportsScreen(
    viewModel: BizViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    val lang = settings.language
    val curr = settings.currency

    // Time filter options: "ALL", "TODAY", "WEEK", "MONTH", "YEAR"
    var timeFilter by remember { mutableStateOf("MONTH") }

    // Filter sales and expenses by date range
    val now = Calendar.getInstance()
    val startFilterTime = when (timeFilter) {
        "TODAY" -> Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        "WEEK" -> Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
        "MONTH" -> Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis
        "YEAR" -> Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -365)
        }.timeInMillis
        else -> 0L
    }

    val filteredSales = sales.filter { it.date >= startFilterTime }
    val filteredExpenses = expenses.filter { it.date >= startFilterTime }

    val totalRevenue = filteredSales.sumOf { it.totalAmount }
    val totalCost = filteredSales.sumOf { it.totalCost }
    val totalSalesProfit = filteredSales.sumOf { it.profit }
    val totalExpensesAmount = filteredExpenses.sumOf { it.amount }
    val netProfit = totalSalesProfit - totalExpensesAmount

    // Resell vs Own Breakdown
    val resellSales = filteredSales.filter { it.serviceType == "RESELL" }
    val ownSales = filteredSales.filter { it.serviceType == "OWN" }
    val resellProfit = resellSales.sumOf { it.profit }
    val ownProfit = ownSales.sumOf { it.profit }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("reports_title", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Business profit, performance, and expense analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        exportCsvReport(context, filteredSales, filteredExpenses, settings, timeFilter)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", fontSize = 12.sp)
                }
            }
        }

        // Time Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "TODAY" to AppStrings.get("filter_today", lang),
                    "WEEK" to AppStrings.get("filter_week", lang),
                    "MONTH" to AppStrings.get("filter_month", lang),
                    "YEAR" to AppStrings.get("filter_year", lang),
                    "ALL" to AppStrings.get("filter_all", lang)
                )
                items(filters) { (key, label) ->
                    val isSel = timeFilter == key
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { timeFilter = key }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Net Profit Grand Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (netProfit >= 0) PrimaryEmerald else AccentRose
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = AppStrings.get("net_profit", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatCurrency(netProfit, curr),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sales Profit: ${FormatCurrency(totalSalesProfit, curr, compact = true)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Expenses: ${FormatCurrency(totalExpensesAmount, curr, compact = true)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Resell Services Profit vs Own Services Profit Breakdown
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(AccentIndigo))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resell Profit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(FormatCurrency(resellProfit, curr, compact = true), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${resellSales.size} sales orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(AccentCyan))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Own Service Profit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(FormatCurrency(ownProfit, curr, compact = true), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${ownSales.size} sales orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Summary Metric Cards
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Gross Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatCurrency(totalRevenue, curr, compact = true), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cost of Goods", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatCurrency(totalCost, curr, compact = true), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("All Expenses", fontSize = 11.sp, color = AccentRose)
                        Text(FormatCurrency(totalExpensesAmount, curr, compact = true), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = AccentRose)
                    }
                }
            }
        }

        // Profit ranking by service
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Profit Ranking by Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        val serviceProfitMap = filteredSales.groupBy { it.serviceName }
            .map { (name, list) ->
                val prf = list.sumOf { it.profit }
                val rev = list.sumOf { it.totalAmount }
                val qty = list.sumOf { it.quantity }
                val type = list.firstOrNull()?.serviceType ?: "OWN"
                name to Triple(prf, rev, qty)
            }.sortedByDescending { it.second.first }

        if (serviceProfitMap.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        "No sales data in this date range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(serviceProfitMap) { (name, stats) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Units sold: ${stats.third} • Total Revenue: ${FormatCurrency(stats.second, curr, compact = true)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(FormatCurrency(stats.first, curr), fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                            Text("Profit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Expense category breakdown
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Expenses by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        val expenseCatMap = filteredExpenses.groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

        if (expenseCatMap.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        "No expenses recorded in this period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(expenseCatMap) { (cat, amount) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        Text(FormatCurrency(amount, curr), fontWeight = FontWeight.Bold, color = AccentRose)
                    }
                }
            }
        }
    }
}

private fun exportCsvReport(
    context: Context,
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    settings: com.example.data.model.BusinessSettings,
    filterName: String
) {
    val csv = buildString {
        appendLine("Business Name,${settings.businessName}")
        appendLine("Report Filter,$filterName")
        appendLine("Currency,${settings.currency}")
        appendLine()
        appendLine("--- SALES & REVENUE ---")
        appendLine("Invoice,Date,Customer,Service,Type,Qty,Sell Price,Subtotal,Discount,Net Total,Paid,Due,Profit")
        sales.forEach { s ->
            appendLine("${s.invoiceNumber},${s.date},\"${s.customerName}\",\"${s.serviceName}\",${s.serviceType},${s.quantity},${s.unitSellPrice},${s.subtotal},${s.discount},${s.totalAmount},${s.paidAmount},${s.dueAmount},${s.profit}")
        }
        appendLine()
        appendLine("--- EXPENSES ---")
        appendLine("Title,Category,Amount,Payment Method,Date,Note")
        expenses.forEach { e ->
            appendLine("\"${e.title}\",\"${e.category}\",${e.amount},${e.paymentMethod},${e.date},\"${e.note}\"")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "${settings.businessName} - Financial Report ($filterName)")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Export Financial Report CSV"))
}
