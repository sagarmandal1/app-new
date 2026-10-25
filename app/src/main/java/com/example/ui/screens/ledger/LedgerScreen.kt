package com.example.ui.screens.ledger

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.local.entity.SupplierEntity
import com.example.data.model.AppStrings
import com.example.ui.components.EmptyState
import com.example.ui.components.FormatCurrency
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.viewmodel.BizViewModel
import com.example.ui.viewmodel.PartyLedgerInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen(
    viewModel: BizViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val duePayments by viewModel.duePayments.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val lang = settings.language
    val curr = settings.currency

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Customers, 1: Suppliers
    var searchQuery by remember { mutableStateOf("") }

    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedSupplierForLedger by remember { mutableStateOf<SupplierEntity?>(null) }

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<Any?>(null) }
    var partyToDelete by remember { mutableStateOf<Any?>(null) }

    var recordDueParty by remember { mutableStateOf<Pair<String, Long>?>(null) } // type ("CUSTOMER"/"SUPPLIER"), id

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
    }
    val filteredSuppliers = suppliers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.company.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    partyToEdit = null
                    showAddPartyDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 72.dp).testTag("fab_add_party")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Party")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("ledger_screen")
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
                        text = AppStrings.get("ledger_title", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track dues, statements, and payment collections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search
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

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${AppStrings.get("customers_tab", lang)} (${customers.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${AppStrings.get("suppliers_tab", lang)} (${suppliers.size})") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                // Customers List
                if (filteredCustomers.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.People,
                        title = "No Customers",
                        description = "Add customers to track sales, receivables, and send payment reminders.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            val ledger = viewModel.getCustomerLedgerInfo(customer.id)
                            PartyCard(
                                name = customer.name,
                                subtext = customer.phone.ifBlank { customer.address },
                                ledger = ledger,
                                currency = curr,
                                isCustomer = true,
                                onOpenLedger = { selectedCustomerForLedger = customer },
                                onCollectOrPay = {
                                    recordDueParty = Pair("CUSTOMER", customer.id)
                                },
                                onEdit = {
                                    partyToEdit = customer
                                    showAddPartyDialog = true
                                },
                                onDelete = { partyToDelete = customer },
                                onCall = {
                                    if (customer.phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Suppliers List
                if (filteredSuppliers.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.People,
                        title = "No Suppliers",
                        description = "Add suppliers to track resell service purchases and dues payable.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSuppliers, key = { it.id }) { supplier ->
                            val ledger = viewModel.getSupplierLedgerInfo(supplier.id)
                            PartyCard(
                                name = supplier.name,
                                subtext = supplier.company.ifBlank { supplier.phone },
                                ledger = ledger,
                                currency = curr,
                                isCustomer = false,
                                onOpenLedger = { selectedSupplierForLedger = supplier },
                                onCollectOrPay = {
                                    recordDueParty = Pair("SUPPLIER", supplier.id)
                                },
                                onEdit = {
                                    partyToEdit = supplier
                                    showAddPartyDialog = true
                                },
                                onDelete = { partyToDelete = supplier },
                                onCall = {
                                    if (supplier.phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Party Add/Edit Dialog
    if (showAddPartyDialog) {
        val isCust = (partyToEdit is CustomerEntity) || (partyToEdit == null && selectedTab == 0)
        AddEditPartyDialog(
            isCustomer = isCust,
            existing = partyToEdit,
            lang = lang,
            onDismiss = { showAddPartyDialog = false },
            onSaveCustomer = { c ->
                viewModel.saveCustomer(c)
                showAddPartyDialog = false
            },
            onSaveSupplier = { s ->
                viewModel.saveSupplier(s)
                showAddPartyDialog = false
            }
        )
    }

    // Customer Detailed Ledger Dialog
    if (selectedCustomerForLedger != null) {
        val c = selectedCustomerForLedger!!
        val cSales = sales.filter { it.customerId == c.id }
        val cPayments = duePayments.filter { it.partyType == "CUSTOMER" && it.partyId == c.id }
        val ledger = viewModel.getCustomerLedgerInfo(c.id)

        DetailedLedgerDialog(
            partyName = c.name,
            partyPhone = c.phone,
            isCustomer = true,
            ledger = ledger,
            currency = curr,
            lang = lang,
            salesHistory = cSales,
            purchasesHistory = emptyList(),
            duePaymentsHistory = cPayments,
            onDismiss = { selectedCustomerForLedger = null },
            onCollectOrPay = {
                recordDueParty = Pair("CUSTOMER", c.id)
                selectedCustomerForLedger = null
            },
            onSendReminder = {
                sendDueReminder(context, c.name, c.phone, ledger.balanceDue, curr, settings.businessName)
            }
        )
    }

    // Supplier Detailed Ledger Dialog
    if (selectedSupplierForLedger != null) {
        val s = selectedSupplierForLedger!!
        val sPurchases = purchases.filter { it.supplierId == s.id }
        val sPayments = duePayments.filter { it.partyType == "SUPPLIER" && it.partyId == s.id }
        val ledger = viewModel.getSupplierLedgerInfo(s.id)

        DetailedLedgerDialog(
            partyName = s.name,
            partyPhone = s.phone,
            isCustomer = false,
            ledger = ledger,
            currency = curr,
            lang = lang,
            salesHistory = emptyList(),
            purchasesHistory = sPurchases,
            duePaymentsHistory = sPayments,
            onDismiss = { selectedSupplierForLedger = null },
            onCollectOrPay = {
                recordDueParty = Pair("SUPPLIER", s.id)
                selectedSupplierForLedger = null
            },
            onSendReminder = {}
        )
    }

    // Record Due Settlement Dialog
    if (recordDueParty != null) {
        val (partyType, partyId) = recordDueParty!!
        val partyName = if (partyType == "CUSTOMER") {
            customers.firstOrNull { it.id == partyId }?.name ?: "Customer"
        } else {
            suppliers.firstOrNull { it.id == partyId }?.name ?: "Supplier"
        }
        val defaultDue = if (partyType == "CUSTOMER") {
            viewModel.getCustomerLedgerInfo(partyId).balanceDue
        } else {
            viewModel.getSupplierLedgerInfo(partyId).balanceDue
        }

        RecordDuePaymentDialog(
            partyType = partyType,
            partyName = partyName,
            initialDue = defaultDue,
            currency = curr,
            paymentMethods = settings.paymentMethods,
            lang = lang,
            onDismiss = { recordDueParty = null },
            onConfirm = { amount, method, note ->
                viewModel.recordDuePayment(partyType, partyId, partyName, amount, method, note)
                recordDueParty = null
            }
        )
    }

    // Delete confirmation
    if (partyToDelete != null) {
        val item = partyToDelete!!
        val name = if (item is CustomerEntity) item.name else (item as SupplierEntity).name
        AlertDialog(
            onDismissRequest = { partyToDelete = null },
            title = { Text(AppStrings.get("delete", lang)) },
            text = { Text("Are you sure you want to remove '$name'? Historic sales and payments will remain in reports.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (item is CustomerEntity) viewModel.deleteCustomer(item)
                        else if (item is SupplierEntity) viewModel.deleteSupplier(item)
                        partyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { partyToDelete = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun PartyCard(
    name: String,
    subtext: String,
    ledger: PartyLedgerInfo,
    currency: String,
    isCustomer: Boolean,
    onOpenLedger: () -> Unit,
    onCollectOrPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLedger() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtext.isNotBlank()) {
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subtext.isNotBlank()) {
                        IconButton(onClick = onCall, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRose, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ledger stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (isCustomer) "Total Billed" else "Total Purchased", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatCurrency(ledger.totalBilled, currency, compact = true),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("Total Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatCurrency(ledger.totalPaid, currency, compact = true),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryEmerald
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isCustomer) "Due Receivable" else "Due Payable", fontSize = 11.sp, color = if (ledger.balanceDue > 0) AccentRose else PrimaryEmerald)
                    Text(
                        text = FormatCurrency(ledger.balanceDue, currency, compact = true),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (ledger.balanceDue > 0) AccentRose else PrimaryEmerald
                    )
                }
            }

            if (ledger.balanceDue > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCollectOrPay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCustomer) PrimaryEmerald else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isCustomer) "Collect Due (+)" else "Pay Due (-)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AddEditPartyDialog(
    isCustomer: Boolean,
    existing: Any?,
    lang: String,
    onDismiss: () -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onSaveSupplier: (SupplierEntity) -> Unit
) {
    var name by remember {
        mutableStateOf(
            if (existing is CustomerEntity) existing.name
            else if (existing is SupplierEntity) existing.name
            else ""
        )
    }
    var phone by remember {
        mutableStateOf(
            if (existing is CustomerEntity) existing.phone
            else if (existing is SupplierEntity) existing.phone
            else ""
        )
    }
    var email by remember {
        mutableStateOf(
            if (existing is CustomerEntity) existing.email
            else if (existing is SupplierEntity) existing.email
            else ""
        )
    }
    var company by remember {
        mutableStateOf(if (existing is SupplierEntity) existing.company else "")
    }
    var address by remember {
        mutableStateOf(
            if (existing is CustomerEntity) existing.address
            else if (existing is SupplierEntity) existing.address
            else ""
        )
    }
    var notes by remember {
        mutableStateOf(
            if (existing is CustomerEntity) existing.notes
            else if (existing is SupplierEntity) existing.notes
            else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isCustomer) (if (existing == null) "Add Customer" else "Edit Customer") else (if (existing == null) "Add Supplier" else "Edit Supplier"))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        if (isCustomer) {
                            val c = (existing as? CustomerEntity) ?: CustomerEntity(name = name)
                            onSaveCustomer(c.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), address = address.trim(), notes = notes.trim()))
                        } else {
                            val s = (existing as? SupplierEntity) ?: SupplierEntity(name = name)
                            onSaveSupplier(s.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), company = company.trim(), address = address.trim(), notes = notes.trim()))
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDuePaymentDialog(
    partyType: String,
    partyName: String,
    initialDue: Double,
    currency: String,
    paymentMethods: List<String>,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    val isCust = partyType == "CUSTOMER"
    var amountStr by remember { mutableStateOf(if (initialDue > 0) initialDue.toString() else "") }
    var paymentMethod by remember { mutableStateOf(paymentMethods.firstOrNull() ?: "Cash") }
    var note by remember { mutableStateOf("") }
    var methodExpanded by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isCust) "Collect Customer Due" else "Pay Supplier Due")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Party: $partyName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current Due: ${FormatCurrency(initialDue, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentRose
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Payment Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        paymentMethods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    paymentMethod = m
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Transaction Ref") },
                    placeholder = { Text("e.g. bKash TrxID #89283") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount > 0) {
                        onConfirm(amount, paymentMethod, note.trim())
                    }
                },
                enabled = amount > 0
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
}

@Composable
fun DetailedLedgerDialog(
    partyName: String,
    partyPhone: String,
    isCustomer: Boolean,
    ledger: PartyLedgerInfo,
    currency: String,
    lang: String,
    salesHistory: List<com.example.data.local.entity.SaleEntity>,
    purchasesHistory: List<com.example.data.local.entity.PurchaseEntity>,
    duePaymentsHistory: List<com.example.data.local.entity.DuePaymentEntity>,
    onDismiss: () -> Unit,
    onCollectOrPay: () -> Unit,
    onSendReminder: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = partyName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCustomer) "Customer Ledger" else "Supplier Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isCustomer && ledger.balanceDue > 0) {
                        IconButton(onClick = onSendReminder) {
                            Icon(Icons.Default.Message, contentDescription = "Send Reminder", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(if (isCustomer) "Billed" else "Purchased", fontSize = 11.sp)
                        Text(FormatCurrency(ledger.totalBilled, currency, compact = true), fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Paid", fontSize = 11.sp)
                        Text(FormatCurrency(ledger.totalPaid, currency, compact = true), fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Balance Due", fontSize = 11.sp, color = AccentRose)
                        Text(FormatCurrency(ledger.balanceDue, currency, compact = true), fontWeight = FontWeight.ExtraBold, color = AccentRose)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Transaction Statements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable statement list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCustomer) {
                        items(salesHistory) { sale ->
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(sale.date))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(sale.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("${sale.invoiceNumber} • $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total: ${FormatCurrency(sale.totalAmount, currency)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        if (sale.dueAmount > 0) {
                                            Text("Due: ${FormatCurrency(sale.dueAmount, currency)}", fontSize = 11.sp, color = AccentRose, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Paid", fontSize = 11.sp, color = PrimaryEmerald)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(purchasesHistory) { pur ->
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(pur.date))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(pur.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("${pur.purchaseNumber} • $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total: ${FormatCurrency(pur.totalAmount, currency)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        if (pur.dueAmount > 0) {
                                            Text("Due: ${FormatCurrency(pur.dueAmount, currency)}", fontSize = 11.sp, color = AccentRose, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Direct settlements
                    items(duePaymentsHistory) { p ->
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(p.date))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryEmerald.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Payment Received (${p.paymentMethod})", fontWeight = FontWeight.SemiBold, color = PrimaryEmerald, fontSize = 13.sp)
                                    Text(p.note.ifBlank { dateStr }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("+ " + FormatCurrency(p.amount, currency), fontWeight = FontWeight.Bold, color = PrimaryEmerald, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                    if (ledger.balanceDue > 0) {
                        Button(
                            onClick = onCollectOrPay,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCustomer) PrimaryEmerald else MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isCustomer) "Collect Due" else "Pay Due")
                        }
                    }
                }
            }
        }
    }
}

private fun sendDueReminder(
    context: Context,
    customerName: String,
    phone: String,
    dueAmount: Double,
    currency: String,
    businessName: String
) {
    val message = "Dear $customerName, your pending balance at $businessName is $currency ${String.format("%.2f", dueAmount)}. Please kindly settle the payment at your earliest convenience. Thank you!"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:$phone")
        putExtra("sms_body", message)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Send Due Reminder"))
    }
}
