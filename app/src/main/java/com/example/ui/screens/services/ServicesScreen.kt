package com.example.ui.screens.services

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ServiceEntity
import com.example.data.model.AppStrings
import com.example.ui.components.EmptyState
import com.example.ui.components.FormatCurrency
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.viewmodel.BizViewModel

@Composable
fun ServicesScreen(
    viewModel: BizViewModel,
    modifier: Modifier = Modifier
) {
    val services by viewModel.services.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    val lang = settings.language
    val curr = settings.currency

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Resell, 2: Own
    var selectedCategory by remember { mutableStateOf("ALL") }

    var serviceToEdit by remember { mutableStateOf<ServiceEntity?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var serviceToDelete by remember { mutableStateOf<ServiceEntity?>(null) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }

    // Filter services
    val filteredServices = services.filter { s ->
        val matchesQuery = s.name.contains(searchQuery, ignoreCase = true) ||
                s.category.contains(searchQuery, ignoreCase = true) ||
                s.supplierName.contains(searchQuery, ignoreCase = true)
        val matchesType = when (selectedTab) {
            1 -> s.type == "RESELL"
            2 -> s.type == "OWN"
            else -> true
        }
        val matchesCat = if (selectedCategory == "ALL") true else s.category == selectedCategory
        matchesQuery && matchesType && matchesCat
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    serviceToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("fab_add_service")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Service")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("services_screen")
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
                        text = AppStrings.get("services_title", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${services.size} services configured",
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

            // Service Type Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(AppStrings.get("all_services", lang)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(AppStrings.get("resell_service", lang)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(AppStrings.get("own_service", lang)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSel = selectedCategory == "ALL"
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategory = "ALL" }
                    ) {
                        Text(
                            text = "All Categories",
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                items(settings.serviceCategories) { cat ->
                    val isSel = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { showCategoryManagerDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Manage Categories",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Manage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Service List
            if (filteredServices.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.BusinessCenter,
                    title = "No Services Found",
                    description = "Tap the + button below to create your first Resell or Own Service.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredServices, key = { it.id }) { service ->
                        ServiceCard(
                            service = service,
                            currency = curr,
                            lang = lang,
                            onEdit = {
                                serviceToEdit = service
                                showAddEditDialog = true
                            },
                            onDelete = {
                                serviceToDelete = service
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Service Dialog
    if (showAddEditDialog) {
        AddEditServiceDialog(
            existing = serviceToEdit,
            currency = curr,
            lang = lang,
            serviceCategories = settings.serviceCategories,
            existingSuppliers = suppliers.map { it.name },
            onDismiss = { showAddEditDialog = false },
            onOpenCategoryManager = { showCategoryManagerDialog = true },
            onSave = { updated ->
                viewModel.saveService(updated)
                showAddEditDialog = false
            }
        )
    }

    // Category Manager Dialog
    if (showCategoryManagerDialog) {
        com.example.ui.components.FullCategoryManagerDialog(
            viewModel = viewModel,
            initialTab = 0,
            onDismiss = { showCategoryManagerDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (serviceToDelete != null) {
        val s = serviceToDelete!!
        AlertDialog(
            onDismissRequest = { serviceToDelete = null },
            title = { Text(AppStrings.get("delete", lang)) },
            text = { Text("Are you sure you want to delete service '${s.name}'? Existing sales records referencing it will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteService(s)
                        serviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToDelete = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun ServiceCard(
    service: ServiceEntity,
    currency: String,
    lang: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    ServiceTypeBadge(type = service.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = service.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRose, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = service.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (service.description.isNotBlank()) {
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (service.type == "RESELL" && service.supplierName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Supplier: ${service.supplierName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Profit Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val costLabel = if (service.type == "RESELL") AppStrings.get("buy_price", lang) else AppStrings.get("making_cost", lang)
                    Text(text = costLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatCurrency(service.costPrice, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(text = AppStrings.get("sell_price", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FormatCurrency(service.sellPrice, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = AppStrings.get("profit_unit", lang), fontSize = 11.sp, color = PrimaryEmerald)
                    Text(
                        text = FormatCurrency(service.profitPerUnit, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryEmerald
                    )
                    Text(
                        text = "${String.format("%.1f", service.marginPercent)}% margin",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryEmerald.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditServiceDialog(
    existing: ServiceEntity?,
    currency: String,
    lang: String,
    serviceCategories: List<String>,
    existingSuppliers: List<String>,
    onDismiss: () -> Unit,
    onOpenCategoryManager: () -> Unit,
    onSave: (ServiceEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "RESELL") }
    var category by remember { mutableStateOf(existing?.category ?: serviceCategories.firstOrNull() ?: "General") }
    var costPriceStr by remember { mutableStateOf(existing?.costPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var sellPriceStr by remember { mutableStateOf(existing?.sellPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var supplierName by remember { mutableStateOf(existing?.supplierName ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }

    var catExpanded by remember { mutableStateOf(false) }

    val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
    val sellPrice = sellPriceStr.toDoubleOrNull() ?: 0.0
    val calculatedProfit = sellPrice - costPrice
    val margin = if (sellPrice > 0) ((sellPrice - costPrice) / sellPrice) * 100 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) AppStrings.get("add_new_service", lang) else AppStrings.get("edit_service", lang))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Service Type Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { type = "RESELL" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (type == "RESELL") AccentIndigo else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = AppStrings.get("resell_service", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "RESELL") androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { type = "OWN" },
                        shape = RoundedCornerShape(8.dp),
                        color = if (type == "OWN") AccentCyan else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = AppStrings.get("own_service", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "OWN") androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("service_name", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Header & Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("category", lang),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "+ Manage Categories",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onOpenCategoryManager() }
                            .padding(4.dp)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Add / Manage Categories", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                catExpanded = false
                                onOpenCategoryManager()
                            }
                        )
                        HorizontalDivider()
                        serviceCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                if (type == "RESELL") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text(AppStrings.get("supplier", lang)) },
                        placeholder = { Text("e.g. Global Tech BD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cost and Sell Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = {
                            Text(if (type == "RESELL") AppStrings.get("buy_price", lang) else AppStrings.get("making_cost", lang))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text(AppStrings.get("sell_price", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Profit Calculation Preview Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Calculated Profit:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatCurrency(calculatedProfit, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryEmerald
                            )
                        }
                        Text(
                            text = "${String.format("%.1f", margin)}% Margin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(AppStrings.get("service_desc", lang)) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val service = ServiceEntity(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            type = type,
                            category = category,
                            costPrice = costPrice,
                            sellPrice = sellPrice,
                            supplierName = if (type == "RESELL") supplierName.trim() else "",
                            status = existing?.status ?: "ACTIVE",
                            description = description.trim(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(service)
                    }
                },
                enabled = name.isNotBlank() && sellPrice > 0
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
