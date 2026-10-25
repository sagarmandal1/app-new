package com.example.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.model.BackupRestoreSummary
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppStrings
import com.example.data.model.BusinessSettings
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.viewmodel.BizViewModel

@Composable
fun SettingsScreen(
    viewModel: BizViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val lang = settings.language

    var showBusinessInfoDialog by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showPaymentMethodsDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    var lastExportedJson by remember { mutableStateOf("") }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var restoreSummary by remember { mutableStateOf<BackupRestoreSummary?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
    var importJsonText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (!content.isNullOrBlank()) {
                    importJsonText = content
                    Toast.makeText(context, "Backup file loaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                restoreErrorMessage = "Failed to read file: ${e.message}"
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = AppStrings.get("nav_settings", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Dynamic configuration, business profile & preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Business Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBusinessInfoDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(settings.businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Owner: ${settings.ownerName} • ${settings.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(AppStrings.get("edit", lang), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Currency & Language Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Currency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(AppStrings.get("currency", lang), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Current: ${settings.currency}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("৳", "$", "€", "₹", "£").forEach { c ->
                                val isSel = settings.currency == c
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable {
                                        viewModel.updateSettings(settings.copy(currency = c))
                                    }
                                ) {
                                    Text(
                                        text = c,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Language Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(AppStrings.get("language", lang), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isEn = settings.language == "en"
                            val isBn = settings.language == "bn"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { viewModel.updateSettings(settings.copy(language = "en")) }
                            ) {
                                Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isEn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { viewModel.updateSettings(settings.copy(language = "bn")) }
                            ) {
                                Text("বাংলা", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isBn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Dark Theme", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(isDarkMode = it)) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Dynamic Categories & Payment Methods Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Category manager
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryManagerDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(AppStrings.get("manage_categories", lang), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("${settings.serviceCategories.size} Service & ${settings.expenseCategories.size} Expense categories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("Manage", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Payment Methods Manager
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPaymentMethodsDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(AppStrings.get("manage_payment_methods", lang), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(settings.paymentMethods.joinToString(", "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        Text("Manage", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Security / PIN Lock Card
        item {
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
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(AppStrings.get("pin_security", lang), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(if (settings.pinEnabled) "PIN Protected" else "PIN Disabled", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = settings.pinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && settings.pinCode.isBlank()) {
                                    showPinDialog = true
                                } else {
                                    viewModel.updateSettings(settings.copy(pinEnabled = enabled))
                                }
                            }
                        )
                    }

                    if (settings.pinEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showPinDialog = true }) {
                            Text("Change PIN Code (Current: ${settings.pinCode})", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Data Backup & Reset Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_restore_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                AppStrings.get("backup_restore", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Full JSON database backup & restore (all 8 tables)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val fullJson = viewModel.exportBackupJson()
                                        lastExportedJson = fullJson
                                        exportBackupFile(context, fullJson, settings.businessName)
                                        showExportSuccessDialog = true
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("export_backup", lang), fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("import_backup", lang), fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showClearDataConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_data_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("clear_data", lang), fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Business Info Dialog
    if (showBusinessInfoDialog) {
        EditBusinessInfoDialog(
            current = settings,
            lang = lang,
            onDismiss = { showBusinessInfoDialog = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showBusinessInfoDialog = false
            }
        )
    }

    // Categories Dialog
    if (showCategoryManagerDialog) {
        com.example.ui.components.FullCategoryManagerDialog(
            viewModel = viewModel,
            initialTab = 0,
            onDismiss = { showCategoryManagerDialog = false }
        )
    }

    // Payment Methods Dialog
    if (showPaymentMethodsDialog) {
        ManagePaymentMethodsDialog(
            currentSettings = settings,
            lang = lang,
            onDismiss = { showPaymentMethodsDialog = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showPaymentMethodsDialog = false
            }
        )
    }

    // PIN Dialog
    if (showPinDialog) {
        ChangePinDialog(
            currentPin = settings.pinCode,
            lang = lang,
            onDismiss = { showPinDialog = false },
            onSave = { newPin ->
                viewModel.updateSettings(settings.copy(pinCode = newPin, pinEnabled = true))
                showPinDialog = false
            }
        )
    }

    // Clear Data Confirmation
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Reset Business Data?") },
            text = { Text("This will delete all sales, purchases, expenses, and ledger history. Service catalog and settings will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Success Dialog
    if (showExportSuccessDialog) {
        BackupExportSuccessDialog(
            jsonString = lastExportedJson,
            onDismiss = { showExportSuccessDialog = false },
            onShareAgain = {
                exportBackupFile(context, lastExportedJson, settings.businessName)
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        ImportBackupDialog(
            initialText = importJsonText,
            onDismiss = { showImportDialog = false },
            onPickFile = { filePickerLauncher.launch("*/*") },
            onRestore = { json, clearExisting ->
                showImportDialog = false
                viewModel.importBackupJson(json, clearExisting) { result ->
                    result.onSuccess { summary ->
                        restoreSummary = summary
                        importJsonText = ""
                    }.onFailure { err ->
                        restoreErrorMessage = err.message ?: "Invalid JSON or database error"
                    }
                }
            }
        )
    }

    // Restore Success Summary Dialog
    restoreSummary?.let { summary ->
        RestoreSummaryDialog(
            summary = summary,
            onDismiss = { restoreSummary = null }
        )
    }

    // Restore Error Dialog
    restoreErrorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { restoreErrorMessage = null },
            title = { Text("Restore Failed", fontWeight = FontWeight.Bold) },
            text = { Text("Failed to restore data from backup.\n\nError: $err\n\nPlease ensure you provide a valid BizLedger JSON backup.") },
            confirmButton = {
                Button(onClick = { restoreErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun EditBusinessInfoDialog(
    current: BusinessSettings,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (BusinessSettings) -> Unit
) {
    var businessName by remember { mutableStateOf(current.businessName) }
    var ownerName by remember { mutableStateOf(current.ownerName) }
    var phone by remember { mutableStateOf(current.phone) }
    var email by remember { mutableStateOf(current.email) }
    var address by remember { mutableStateOf(current.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("business_info", lang)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business / Store Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Business Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Business Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Business Address") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        current.copy(
                            businessName = businessName.trim().ifBlank { current.businessName },
                            ownerName = ownerName.trim().ifBlank { current.ownerName },
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim()
                        )
                    )
                }
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
fun ManagePaymentMethodsDialog(
    currentSettings: BusinessSettings,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (BusinessSettings) -> Unit
) {
    var methods by remember { mutableStateOf(currentSettings.paymentMethods.toMutableList()) }
    var newMethod by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("manage_payment_methods", lang)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newMethod,
                        onValueChange = { newMethod = it },
                        placeholder = { Text("e.g. Upay, SureCash, Card") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (newMethod.isNotBlank() && !methods.contains(newMethod.trim())) {
                            methods.add(newMethod.trim())
                            newMethod = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    methods.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m, fontSize = 13.sp)
                            if (methods.size > 1) {
                                IconButton(onClick = { methods.remove(m) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = AccentRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(currentSettings.copy(paymentMethods = methods.toList()))
            }) {
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
fun ChangePinDialog(
    currentPin: String,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf(currentPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 4-Digit Owner PIN") },
        text = {
            Column {
                Text("Enter a 4-digit numeric PIN to secure your business ledger upon launch.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter { ch -> ch.isDigit() } },
                    label = { Text("PIN Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (pin.length >= 4) onSave(pin) },
                enabled = pin.length >= 4
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

private fun exportBackupFile(context: Context, jsonString: String, businessName: String) {
    try {
        val fileName = "bizledger_backup_${System.currentTimeMillis()}.json"
        val file = java.io.File(context.cacheDir, fileName)
        file.writeText(jsonString)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "BizLedger Complete Backup - $businessName")
            putExtra(Intent.EXTRA_TEXT, jsonString)
        }
        val chooser = Intent.createChooser(intent, "Save or Export BizLedger Full Backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {}
}

@Composable
fun BackupExportSuccessDialog(
    jsonString: String,
    onDismiss: () -> Unit,
    onShareAgain: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryEmerald,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text("Full Backup Ready!", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "All 8 tables (Services, Customers, Suppliers, Sales, Purchases, Expenses, Due Ledger, and Settings) have been successfully compiled into real JSON format.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = jsonString.take(350) + if (jsonString.length > 350) "\n... [Full JSON ${jsonString.length} chars]" else "",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("BizLedger Backup", jsonString)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = onShareAgain) {
                Text("Share File")
            }
        }
    )
}

@Composable
fun ImportBackupDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onRestore: (json: String, clearExisting: Boolean) -> Unit
) {
    var jsonText by remember { mutableStateOf(initialText) }
    var clearExisting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Restore Backup (JSON)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Paste your BizLedger JSON backup or select a .json backup file from device storage:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select .json File from Storage", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("Backup JSON Content") },
                    placeholder = { Text("{\"app\": \"BizLedger\", \"services\": [...], ...}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { clearExisting = !clearExisting }
                ) {
                    Checkbox(checked = clearExisting, onCheckedChange = { clearExisting = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wipe existing data before restore", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonText.isNotBlank()) {
                        onRestore(jsonText.trim(), clearExisting)
                    }
                },
                enabled = jsonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Text("Restore Database")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestoreSummaryDialog(
    summary: BackupRestoreSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryEmerald,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text("Data Restored Successfully!", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "All records have been restored into the local BizLedger database:",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Services: ${summary.servicesCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Customers: ${summary.customersCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Suppliers: ${summary.suppliersCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Sales: ${summary.salesCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Purchases: ${summary.purchasesCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Expenses: ${summary.expensesCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Due Payments: ${summary.duePaymentsCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("• Business Settings & Preferences: Updated", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
