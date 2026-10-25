package com.example.ui.screens.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ServiceEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.model.AppStrings
import com.example.ui.components.FormatCurrency
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseDialog(
    services: List<ServiceEntity>,
    suppliers: List<SupplierEntity>,
    paymentMethods: List<String>,
    currency: String,
    lang: String,
    onDismiss: () -> Unit,
    onQuickAddSupplier: (SupplierEntity, (Long) -> Unit) -> Unit,
    onSavePurchase: (Long, String, Long, String, Int, Double, Double, String, String) -> Unit
) {
    val resellServices = services.filter { it.type == "RESELL" }

    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(suppliers.firstOrNull()) }
    var selectedService by remember { mutableStateOf<ServiceEntity?>(resellServices.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("1") }
    var buyPriceStr by remember { mutableStateOf(selectedService?.costPrice?.toString() ?: "0") }
    var paidAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(paymentMethods.firstOrNull() ?: "Cash") }
    var note by remember { mutableStateOf("") }

    var supplierExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }
    var showQuickAddSupplier by remember { mutableStateOf(false) }

    val qty = quantityStr.toIntOrNull() ?: 1
    val buyPrice = buyPriceStr.toDoubleOrNull() ?: 0.0
    val totalAmount = qty * buyPrice
    val paid = if (paidAmountStr.isBlank()) totalAmount else (paidAmountStr.toDoubleOrNull() ?: 0.0)
    val due = maxOf(0.0, totalAmount - paid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("new_purchase", lang)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Supplier Selector + Quick Add
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ExposedDropdownMenuBox(
                        expanded = supplierExpanded,
                        onExpandedChange = { supplierExpanded = !supplierExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "Select Supplier",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStrings.get("supplier", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = supplierExpanded,
                            onDismissRequest = { supplierExpanded = false }
                        ) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = { Text("${sup.name} (${sup.company.ifBlank { sup.phone }})") },
                                    onClick = {
                                        selectedSupplier = sup
                                        supplierExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { showQuickAddSupplier = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Supplier", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Resell Service Selector
                ExposedDropdownMenuBox(
                    expanded = serviceExpanded,
                    onExpandedChange = { serviceExpanded = !serviceExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedService?.name ?: "Select Resell Service",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service (Resell)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = serviceExpanded,
                        onDismissRequest = { serviceExpanded = false }
                    ) {
                        resellServices.forEach { srv ->
                            DropdownMenuItem(
                                text = { Text("${srv.name} (Cost: $currency ${srv.costPrice})") },
                                onClick = {
                                    selectedService = srv
                                    buyPriceStr = srv.costPrice.toString()
                                    serviceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text(AppStrings.get("quantity", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text(AppStrings.get("buy_price", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = paidAmountStr,
                        onValueChange = { paidAmountStr = it },
                        label = { Text(AppStrings.get("paid_amount", lang)) },
                        placeholder = { Text(FormatCurrency(totalAmount, currency, compact = true)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = !methodExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Method") },
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Purchase: ${FormatCurrency(totalAmount, currency)}", fontWeight = FontWeight.Bold)
                            if (due > 0) {
                                Text("Payable Due: ${FormatCurrency(due, currency)}", color = AccentRose, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Full Paid", color = PrimaryEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val srv = selectedService
                    val sup = selectedSupplier
                    if (srv != null && sup != null) {
                        onSavePurchase(
                            sup.id,
                            sup.name,
                            srv.id,
                            srv.name,
                            qty,
                            buyPrice,
                            paid,
                            paymentMethod,
                            note
                        )
                    }
                },
                enabled = selectedSupplier != null && selectedService != null && totalAmount > 0
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

    if (showQuickAddSupplier) {
        QuickAddSupplierDialog(
            lang = lang,
            onDismiss = { showQuickAddSupplier = false },
            onSave = { newSup ->
                onQuickAddSupplier(newSup) { id ->
                    selectedSupplier = newSup.copy(id = id)
                    showQuickAddSupplier = false
                }
            }
        )
    }
}

@Composable
fun QuickAddSupplierDialog(
    lang: String,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_supplier", lang)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier Name") },
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(SupplierEntity(name = name.trim(), phone = phone.trim(), company = company.trim()))
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
