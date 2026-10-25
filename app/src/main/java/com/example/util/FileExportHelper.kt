package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.model.BusinessSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileExportHelper {

    /**
     * Generates a real styled A4 PDF document invoice and returns the File.
     */
    fun generateInvoicePdf(
        context: Context,
        sale: SaleEntity,
        settings: BusinessSettings
    ): File {
        val invoicesDir = File(context.cacheDir, "invoices")
        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs()
        }
        val pdfFile = File(invoicesDir, "Invoice_${sale.invoiceNumber}.pdf")

        val document = PdfDocument()
        // Standard A4: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Background
        canvas.drawColor(Color.WHITE)

        // Top decorative header bar
        paint.color = Color.parseColor("#0F766E") // Emerald green theme
        canvas.drawRect(0f, 0f, 595f, 18f, paint)

        // Business Name & Profile Header
        paint.color = Color.parseColor("#111827")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val bizName = if (settings.businessName.isNotBlank()) settings.businessName else "BizLedger Services"
        canvas.drawText(bizName, 40f, 60f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")
        var currentY = 76f
        if (settings.phone.isNotBlank()) {
            canvas.drawText("Phone: ${settings.phone}", 40f, currentY, paint)
            currentY += 14f
        }
        if (settings.email.isNotBlank()) {
            canvas.drawText("Email: ${settings.email}", 40f, currentY, paint)
            currentY += 14f
        }
        if (settings.address.isNotBlank()) {
            canvas.drawText("Address: ${settings.address}", 40f, currentY, paint)
            currentY += 14f
        }

        // Right-aligned INVOICE badge
        paint.color = Color.parseColor("#0F766E")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TAX INVOICE", 430f, 60f, paint)

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.date))
        paint.color = Color.parseColor("#374151")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Invoice #: ${sale.invoiceNumber}", 430f, 78f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#6B7280")
        canvas.drawText("Date: $dateStr", 430f, 92f, paint)
        canvas.drawText("Payment: ${sale.paymentMethod}", 430f, 106f, paint)

        // Divider
        currentY = maxOf(currentY + 10f, 130f)
        paint.color = Color.parseColor("#E5E7EB")
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        paint.strokeWidth = 1f

        // Billed To Box
        currentY += 24f
        paint.color = Color.parseColor("#F9FAFB")
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 46f, 8f, 8f, paint)

        paint.color = Color.parseColor("#0F766E")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO / CUSTOMER", 55f, currentY + 18f, paint)

        paint.color = Color.parseColor("#111827")
        paint.textSize = 12f
        canvas.drawText(sale.customerName, 55f, currentY + 34f, paint)

        // Line Items Table Header
        currentY += 70f
        paint.color = Color.parseColor("#0F766E")
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 26f, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DESCRIPTION / SERVICE", 55f, currentY + 17f, paint)
        canvas.drawText("TYPE", 270f, currentY + 17f, paint)
        canvas.drawText("QTY", 340f, currentY + 17f, paint)
        canvas.drawText("RATE (${settings.currency})", 390f, currentY + 17f, paint)
        canvas.drawText("AMOUNT (${settings.currency})", 475f, currentY + 17f, paint)

        // Line Item Row
        currentY += 44f
        paint.color = Color.parseColor("#111827")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(sale.serviceName, 55f, currentY, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#4B5563")
        canvas.drawText(sale.serviceType, 270f, currentY, paint)
        canvas.drawText("${sale.quantity}", 345f, currentY, paint)
        canvas.drawText(String.format(Locale.US, "%.2f", sale.unitSellPrice), 390f, currentY, paint)
        canvas.drawText(String.format(Locale.US, "%.2f", sale.subtotal), 475f, currentY, paint)

        // Divider
        currentY += 16f
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawLine(40f, currentY, 555f, currentY, paint)

        // Summary Calculations (right-aligned box)
        currentY += 28f
        val summaryXLabel = 360f
        val summaryXValue = 485f

        paint.textSize = 10f
        paint.color = Color.parseColor("#4B5563")
        canvas.drawText("Subtotal:", summaryXLabel, currentY, paint)
        canvas.drawText("${settings.currency} " + String.format(Locale.US, "%.2f", sale.subtotal), summaryXValue, currentY, paint)

        if (sale.discount > 0.0) {
            currentY += 18f
            paint.color = Color.parseColor("#DC2626") // Red discount
            canvas.drawText("Discount:", summaryXLabel, currentY, paint)
            canvas.drawText("- ${settings.currency} " + String.format(Locale.US, "%.2f", sale.discount), summaryXValue, currentY, paint)
        }

        currentY += 22f
        paint.color = Color.parseColor("#111827")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Net Total:", summaryXLabel, currentY, paint)
        canvas.drawText("${settings.currency} " + String.format(Locale.US, "%.2f", sale.totalAmount), summaryXValue, currentY, paint)

        currentY += 18f
        paint.color = Color.parseColor("#059669")
        paint.textSize = 11f
        canvas.drawText("Paid Amount:", summaryXLabel, currentY, paint)
        canvas.drawText("${settings.currency} " + String.format(Locale.US, "%.2f", sale.paidAmount), summaryXValue, currentY, paint)

        currentY += 20f
        if (sale.dueAmount > 0.0) {
            paint.color = Color.parseColor("#DC2626")
            paint.textSize = 12f
            canvas.drawText("Remaining Due:", summaryXLabel, currentY, paint)
            canvas.drawText("${settings.currency} " + String.format(Locale.US, "%.2f", sale.dueAmount), summaryXValue, currentY, paint)
        } else {
            paint.color = Color.parseColor("#059669")
            paint.textSize = 11f
            canvas.drawText("Status:", summaryXLabel, currentY, paint)
            canvas.drawText("PAID IN FULL", summaryXValue, currentY, paint)
        }

        // Note section if exists
        if (sale.note.isNotBlank()) {
            currentY += 40f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText("NOTE / REMARKS:", 55f, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            paint.color = Color.parseColor("#374151")
            canvas.drawText(sale.note, 55f, currentY + 14f, paint)
        }

        // Footer Thank You
        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Thank you for your business! • Generated by BizLedger", 170f, 790f, paint)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Shares the generated real PDF invoice file via Android Sharesheet with read URI permissions.
     */
    fun shareInvoicePdf(
        context: Context,
        sale: SaleEntity,
        settings: BusinessSettings
    ) {
        try {
            val pdfFile = generateInvoicePdf(context, sale, settings)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${sale.invoiceNumber} - ${settings.businessName}")
                putExtra(Intent.EXTRA_TEXT, "Invoice ${sale.invoiceNumber} from ${settings.businessName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Invoice PDF via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Writes financial reports to a real .csv file and shares it via FileProvider.
     */
    fun generateAndShareReportsCsv(
        context: Context,
        sales: List<SaleEntity>,
        expenses: List<ExpenseEntity>,
        settings: BusinessSettings,
        filterName: String
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeFilter = filterName.replace(" ", "_").lowercase()
            val csvFile = File(reportsDir, "bizledger_report_${safeFilter}_$timestamp.csv")

            val csvContent = buildString {
                appendLine("Business Name,${settings.businessName}")
                appendLine("Owner,${settings.ownerName}")
                appendLine("Report Period,$filterName")
                appendLine("Exported At,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("Currency,${settings.currency}")
                appendLine()
                appendLine("--- SALES & REVENUES ---")
                appendLine("Invoice No,Date,Customer,Service,Type,Qty,Sell Price,Subtotal,Discount,Total Amount,Paid Amount,Due Amount,Profit,Payment Method")
                sales.forEach { s ->
                    val d = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(s.date))
                    appendLine("\"${s.invoiceNumber}\",\"$d\",\"${s.customerName}\",\"${s.serviceName}\",\"${s.serviceType}\",${s.quantity},${s.unitSellPrice},${s.subtotal},${s.discount},${s.totalAmount},${s.paidAmount},${s.dueAmount},${s.profit},\"${s.paymentMethod}\"")
                }
                appendLine()
                appendLine("--- EXPENSES ---")
                appendLine("Title,Category,Amount,Payment Method,Date,Note")
                expenses.forEach { e ->
                    val d = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(e.date))
                    appendLine("\"${e.title}\",\"${e.category}\",${e.amount},\"${e.paymentMethod}\",\"$d\",\"${e.note}\"")
                }
            }

            csvFile.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${settings.businessName} - Financial Report ($filterName)")
                putExtra(Intent.EXTRA_TEXT, "Attached BizLedger Financial Report ($filterName) CSV file.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Financial Report CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares backup JSON file via FileProvider.
     */
    fun shareBackupJsonFile(
        context: Context,
        jsonString: String,
        businessName: String
    ) {
        try {
            val fileName = "bizledger_backup_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)
            file.writeText(jsonString)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BizLedger Full Backup - $businessName")
                putExtra(Intent.EXTRA_TEXT, jsonString)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Save or Share BizLedger Backup File")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
