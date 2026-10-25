package com.example.ui.screens.sales

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.ServiceEntity
import com.example.data.model.AppStrings
import com.example.ui.components.EmptyState
import com.example.ui.components.FormatCurrency
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SecondaryTeal
import com.example.ui.viewmodel.BizViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen(
    viewModel: BizViewModel,
    showCreateDialogInitially: Boolean = false,
    onCloseCreateDialog: () -> Unit = {}
) {
    val sales by viewModel.sales.collectAsState()
    val activeServices by viewModel.activeServices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val lang = settings.language
    val curr = settings.currency

    var searchQuery by remember { mutableStateOf("") }
    var showCreateSaleDialog by remember { mutableStateOf(showCreateDialogInitially) }
    var selectedSaleForInvoice by remember { mutableStateOf<SaleEntity?>(null) }
    var saleToDelete by remember { mutableStateOf<SaleEntity?>(null) }

    val filteredSales = sales.filter { s ->
        s.customerName.contains(searchQuery, ignoreCase = true) ||
                s.serviceName.contains(searchQuery, ignoreCase = true) ||
                s.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                s.paymentMethod.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSaleDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("fab_create_sale")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Sale")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("sales_screen")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("sales_title", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total ${sales.size} sales recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(AppStrings.get("search", lang)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredSales.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ShoppingCart,
                    title = "No Sales Recorded",
                    description = "Record your customer orders to track income, auto profit, and dues.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales, key = { it.id }) { sale ->
                        SaleCard(
                            sale = sale,
                            currency = curr,
                            lang = lang,
                            onViewInvoice = { selectedSaleForInvoice = sale },
                            onDelete = { saleToDelete = sale }
                        )
                    }
                }
            }
        }
    }

    // Create Sale Dialog
    if (showCreateSaleDialog) {
        CreateSaleDialog(
            activeServices = activeServices,
            customers = customers,
            paymentMethods = settings.paymentMethods,
            currency = curr,
            lang = lang,
            onDismiss = {
                showCreateSaleDialog = false
                onCloseCreateDialog()
            },
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
                ) { createdSale ->
                    showCreateSaleDialog = false
                    onCloseCreateDialog()
                    selectedSaleForInvoice = createdSale
                }
            }
        )
    }

    // Invoice Dialog
    if (selectedSaleForInvoice != null) {
        InvoiceDialog(
            sale = selectedSaleForInvoice!!,
            settings = settings,
            onDismiss = { selectedSaleForInvoice = null }
        )
    }

    // Delete Sale Confirmation
    if (saleToDelete != null) {
        val s = saleToDelete!!
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text(AppStrings.get("delete", lang)) },
            text = { Text("Delete sale '${s.invoiceNumber}' for ${s.customerName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale(s)
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun SaleCard(
    sale: SaleEntity,
    currency: String,
    lang: String,
    onViewInvoice: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = sale.invoiceNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ServiceTypeBadge(type = sale.serviceType)
                }

                Row {
                    IconButton(onClick = onViewInvoice, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Receipt, contentDescription = "Invoice", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRose, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = sale.serviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Customer: ${sale.customerName} • Qty: ${sale.quantity} • $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Billed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatCurrency(sale.totalAmount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("Paid (${sale.paymentMethod})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatCurrency(sale.paidAmount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryEmerald
                    )
                }

                if (sale.dueAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Due", fontSize = 11.sp, color = AccentRose)
                        Text(
                            text = FormatCurrency(sale.dueAmount, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentRose
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Profit", fontSize = 11.sp, color = SecondaryTeal)
                    Text(
                        text = FormatCurrency(sale.profit, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SecondaryTeal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleDialog(
    activeServices: List<ServiceEntity>,
    customers: List<CustomerEntity>,
    paymentMethods: List<String>,
    currency: String,
    lang: String,
    onDismiss: () -> Unit,
    onSaveCustomer: (CustomerEntity, (Long) -> Unit) -> Unit,
    onSaveSale: (Long, String, ServiceEntity, Int, Double, Double, Double, String, String) -> Unit
) {
    var selectedService by remember { mutableStateOf<ServiceEntity?>(activeServices.firstOrNull()) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(customers.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("1") }
    var sellPriceStr by remember { mutableStateOf(selectedService?.sellPrice?.toString() ?: "0") }
    var discountStr by remember { mutableStateOf("0") }
    var paidAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(paymentMethods.firstOrNull() ?: "Cash") }
    var note by remember { mutableStateOf("") }

    var serviceMenuExpanded by remember { mutableStateOf(false) }
    var customerMenuExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    var showQuickAddCustomer by remember { mutableStateOf(false) }

    val qty = quantityStr.toIntOrNull() ?: 1
    val unitPrice = sellPriceStr.toDoubleOrNull() ?: 0.0
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val subtotal = qty * unitPrice
    val total = maxOf(0.0, subtotal - discount)
    val paid = if (paidAmountStr.isBlank()) total else (paidAmountStr.toDoubleOrNull() ?: 0.0)
    val due = maxOf(0.0, total - paid)
    val costPrice = selectedService?.costPrice ?: 0.0
    val totalCost = qty * costPrice
    val profit = total - totalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("new_sale", lang)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Customer selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = customerMenuExpanded,
                        onExpandedChange = { customerMenuExpanded = !customerMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStrings.get("customer", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = customerMenuExpanded,
                            onDismissRequest = { customerMenuExpanded = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${c.phone})") },
                                    onClick = {
                                        selectedCustomer = c
                                        customerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { showQuickAddCustomer = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Customer", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Service Selector
                ExposedDropdownMenuBox(
                    expanded = serviceMenuExpanded,
                    onExpandedChange = { serviceMenuExpanded = !serviceMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedService?.let { "${it.name} (${it.type})" } ?: "Select Service",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("select_service", lang)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = serviceMenuExpanded,
                        onDismissRequest = { serviceMenuExpanded = false }
                    ) {
                        activeServices.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${s.name} (${s.type})")
                                        Text("Cost: $currency ${s.costPrice} • Sell: $currency ${s.sellPrice}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedService = s
                                    sellPriceStr = s.sellPrice.toString()
                                    serviceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity and Unit Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text(AppStrings.get("quantity", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text(AppStrings.get("sell_price", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Discount and Paid Amount Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it },
                        label = { Text(AppStrings.get("discount", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = paidAmountStr,
                        onValueChange = { paidAmountStr = it },
                        label = { Text(AppStrings.get("paid_amount", lang)) },
                        placeholder = { Text(FormatCurrency(total, currency, compact = true)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Method Selector
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("payment_method", lang)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Calculation Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Payable: ${FormatCurrency(total, currency)}", fontWeight = FontWeight.Bold)
                            if (due > 0) {
                                Text("Due: ${FormatCurrency(due, currency)}", color = AccentRose, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Full Paid", color = PrimaryEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Auto Profit: ${FormatCurrency(profit, currency)} (Cost: ${FormatCurrency(totalCost, currency)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryEmerald
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val srv = selectedService
                    val cust = selectedCustomer
                    if (srv != null && cust != null) {
                        onSaveSale(
                            cust.id,
                            cust.name,
                            srv,
                            qty,
                            unitPrice,
                            discount,
                            paid,
                            paymentMethod,
                            note
                        )
                    }
                },
                enabled = selectedService != null && selectedCustomer != null && total >= 0
            ) {
                Text(AppStrings.get("confirm", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )

    // Quick Add Customer Sub-Dialog
    if (showQuickAddCustomer) {
        QuickAddCustomerDialog(
            lang = lang,
            onDismiss = { showQuickAddCustomer = false },
            onSave = { newCust ->
                onSaveCustomer(newCust) { id ->
                    selectedCustomer = newCust.copy(id = id)
                    showQuickAddCustomer = false
                }
            }
        )
    }
}

@Composable
fun QuickAddCustomerDialog(
    lang: String,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_customer", lang)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(CustomerEntity(name = name.trim(), phone = phone.trim()))
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@Composable
fun InvoiceDialog(
    sale: SaleEntity,
    settings: com.example.data.model.BusinessSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.date))
    val curr = settings.currency

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = settings.businessName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${settings.phone} • ${settings.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "BILL / INVOICE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Invoice metadata
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Invoice #: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Date: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Bill To: ${sale.customerName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Method: ${sale.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Line Item Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sale.serviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(FormatCurrency(sale.subtotal, curr), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Qty: ${sale.quantity} × ${FormatCurrency(sale.unitSellPrice, curr)} (${sale.serviceType})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Totals Breakdown
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                        Text(FormatCurrency(sale.subtotal, curr), style = MaterialTheme.typography.bodySmall)
                    }
                    if (sale.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount", style = MaterialTheme.typography.bodySmall, color = AccentRose)
                            Text("- " + FormatCurrency(sale.discount, curr), style = MaterialTheme.typography.bodySmall, color = AccentRose)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(FormatCurrency(sale.totalAmount, curr), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Paid", color = PrimaryEmerald, style = MaterialTheme.typography.bodySmall)
                        Text(FormatCurrency(sale.paidAmount, curr), color = PrimaryEmerald, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    if (sale.dueAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Due", color = AccentRose, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(FormatCurrency(sale.dueAmount, curr), color = AccentRose, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            shareInvoiceText(context, sale, settings)
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Invoice")
                    }
                }
            }
        }
    }
}

private fun shareInvoiceText(
    context: Context,
    sale: SaleEntity,
    settings: com.example.data.model.BusinessSettings
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.date))
    val curr = settings.currency
    val text = buildString {
        appendLine("===============================")
        appendLine("🧾 INVOICE - ${settings.businessName}")
        appendLine("📞 ${settings.phone} | ${settings.address}")
        appendLine("===============================")
        appendLine("Invoice No: ${sale.invoiceNumber}")
        appendLine("Date: $dateStr")
        appendLine("Customer: ${sale.customerName}")
        appendLine("-------------------------------")
        appendLine("Item: ${sale.serviceName}")
        appendLine("Type: ${sale.serviceType}")
        appendLine("Qty: ${sale.quantity} × $curr ${sale.unitSellPrice} = $curr ${sale.subtotal}")
        if (sale.discount > 0) {
            appendLine("Discount: - $curr ${sale.discount}")
        }
        appendLine("Total Amount: $curr ${sale.totalAmount}")
        appendLine("Paid Amount: $curr ${sale.paidAmount} (${sale.paymentMethod})")
        if (sale.dueAmount > 0) {
            appendLine("Remaining Due: $curr ${sale.dueAmount}")
        } else {
            appendLine("Status: PAID IN FULL ✅")
        }
        appendLine("===============================")
        appendLine("Thank you for your business!")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Invoice ${sale.invoiceNumber} from ${settings.businessName}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Invoice via"))
}
