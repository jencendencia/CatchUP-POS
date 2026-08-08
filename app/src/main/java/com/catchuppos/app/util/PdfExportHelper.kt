package com.catchuppos.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.catchuppos.app.data.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds simple, readable A4 PDFs using the platform [PdfDocument] API
 * (no external dependencies) and shares them through the system share sheet.
 */
// A4 page layout constants (file-scoped so the PdfWriter below can use them)
private const val PDF_PAGE_W = 595
private const val PDF_PAGE_H = 842
private const val PDF_MARGIN = 36f
private const val PDF_RIGHT = PDF_PAGE_W - PDF_MARGIN
private const val PDF_BOTTOM = PDF_PAGE_H - PDF_MARGIN

object PdfExportHelper {

    // ═══════════════════════════════════════════════════════════════
    // Transactions export
    // ═══════════════════════════════════════════════════════════════

    fun buildTransactionsPdf(
        rangeLabel: String,
        transactions: List<TransactionEntity>,
        totalOrders: Int,
        totalSales: Double,
        totalCash: Double,
        totalGcash: Double,
        totalItems: Int
    ): ByteArray {
        val doc = PdfDocument()
        val w = PdfWriter(doc)

        w.header("CatchUp POS", "Transactions Report — $rangeLabel", "Generated ${now()}")
        w.kv("Total Orders", "$totalOrders")
        w.kv("Total Sales", money(totalSales))
        w.kv("Total Cash", money(totalCash))
        w.kv("Total GCash", money(totalGcash))
        w.kv("Total Items Sold", "$totalItems")
        w.spacer(4f)

        w.section("TRANSACTIONS (${transactions.size})")
        val cols = listOf(
            Col("ORDER #", 36f, 55f),
            Col("CUSTOMER", 91f, 95f),
            Col("ITEMS", 186f, 130f),
            Col("TOTAL", 316f, 70f),
            Col("PAYMENT", 386f, 55f),
            Col("STATUS", 441f, 60f),
            Col("TIME", 501f, 58f)
        )
        w.tableHeader(cols)
        if (transactions.isEmpty()) {
            w.note("No transactions found")
        } else {
            transactions.forEach { t ->
                w.tableRow(
                    cols,
                    listOf(
                        "#${String.format(Locale.US, "%05d", t.id)}",
                        t.customerName.ifBlank { "—" },
                        t.itemsJson,
                        money(t.total),
                        t.paymentMethod,
                        t.status,
                        SimpleDateFormat("h:mm a", Locale.US).format(Date(t.createdAt))
                    )
                )
            }
        }

        w.finish()
        return toBytes(doc)
    }

    // ═══════════════════════════════════════════════════════════════
    // Reports export (full sales report for the selected date range)
    // ═══════════════════════════════════════════════════════════════

    fun buildReportPdf(
        rangeLabel: String,
        totalSales: Double,
        totalOrders: Int,
        avgOrderValue: Double,
        totalItemsSold: Int,
        grossProfit: Double,
        hourlySales: List<HourlySalesSummary>,
        dailySales: List<DailySalesSummary>,
        paymentMethods: List<PaymentMethodSales>,
        topProducts: List<TopSellingProduct>,
        recentTransactions: List<TransactionEntity>,
        orderStatusCounts: List<OrderStatusSummary>,
        categorySales: List<CategorySalesSummary>,
        cashierPerformance: List<CashierSummary>
    ): ByteArray {
        val doc = PdfDocument()
        val w = PdfWriter(doc)

        w.header("CatchUp POS", "Sales Report — $rangeLabel", "Generated ${now()}")

        w.section("SUMMARY")
        w.kv("Total Sales", money(totalSales))
        w.kv("Total Orders", "$totalOrders")
        w.kv("Average Order Value", money(avgOrderValue))
        w.kv("Total Items Sold", "$totalItemsSold")
        w.kv("Gross Profit", money(grossProfit))

        if (hourlySales.isNotEmpty()) {
            w.section("HOURLY SALES")
            val cols = listOf(Col("HOUR", 36f, 100f), Col("SALES", 140f, 200f))
            w.tableHeader(cols)
            hourlySales.sortedBy { it.hour }.forEach { h ->
                w.tableRow(cols, listOf(String.format(Locale.US, "%02d:00", h.hour), money(h.amount)))
            }
        }

        if (dailySales.isNotEmpty()) {
            w.section("DAILY SALES")
            val cols = listOf(Col("DATE", 36f, 90f), Col("SALES", 130f, 140f), Col("ORDERS", 274f, 70f))
            w.tableHeader(cols)
            dailySales.forEach { d ->
                w.tableRow(
                    cols,
                    listOf(
                        SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(d.dayOffset * 86400000L)),
                        money(d.total),
                        "${d.orderCount}"
                    )
                )
            }
        }

        if (paymentMethods.isNotEmpty()) {
            w.section("PAYMENT METHODS")
            val total = paymentMethods.sumOf { it.totalSales }
            val cols = listOf(Col("METHOD", 36f, 90f), Col("SALES", 130f, 140f), Col("%", 274f, 70f))
            w.tableHeader(cols)
            paymentMethods.forEach { pm ->
                val pct = if (total > 0) pm.totalSales / total * 100 else 0.0
                w.tableRow(cols, listOf(pm.method, money(pm.totalSales), String.format(Locale.US, "%.1f%%", pct)))
            }
        }

        if (topProducts.isNotEmpty()) {
            w.section("TOP SELLING PRODUCTS")
            val cols = listOf(Col("PRODUCT", 36f, 260f), Col("QTY", 300f, 50f), Col("SALES", 354f, 100f))
            w.tableHeader(cols)
            topProducts.forEach { p ->
                w.tableRow(cols, listOf(p.productName, "${p.totalQty}", money(p.totalSales)))
            }
        }

        if (categorySales.isNotEmpty()) {
            w.section("SALES BY CATEGORY")
            val total = categorySales.sumOf { it.totalSales }
            val cols = listOf(Col("CATEGORY", 36f, 140f), Col("SALES", 180f, 140f), Col("%", 324f, 70f))
            w.tableHeader(cols)
            categorySales.forEach { cs ->
                val pct = if (total > 0) cs.totalSales / total * 100 else 0.0
                w.tableRow(cols, listOf(cs.category, money(cs.totalSales), String.format(Locale.US, "%.1f%%", pct)))
            }
        }

        if (orderStatusCounts.isNotEmpty()) {
            w.section("ORDER STATUS")
            val cols = listOf(Col("STATUS", 36f, 120f), Col("COUNT", 160f, 70f))
            w.tableHeader(cols)
            orderStatusCounts.forEach { s ->
                w.tableRow(cols, listOf(s.status, "${s.count}"))
            }
        }

        if (cashierPerformance.isNotEmpty()) {
            w.section("CASHIER PERFORMANCE")
            val cols = listOf(Col("CASHIER", 36f, 180f), Col("ORDERS", 220f, 70f), Col("SALES", 294f, 120f))
            w.tableHeader(cols)
            cashierPerformance.forEach { c ->
                w.tableRow(cols, listOf(c.cashierName, "${c.orderCount}", money(c.totalSales)))
            }
        }

        if (recentTransactions.isNotEmpty()) {
            w.section("RECENT TRANSACTIONS")
            val cols = listOf(
                Col("ORDER #", 36f, 55f),
                Col("CUSTOMER", 91f, 95f),
                Col("ITEMS", 186f, 130f),
                Col("TOTAL", 316f, 70f),
                Col("PAYMENT", 386f, 55f),
                Col("STATUS", 441f, 60f),
                Col("TIME", 501f, 58f)
            )
            w.tableHeader(cols)
            recentTransactions.take(5).forEach { t ->
                w.tableRow(
                    cols,
                    listOf(
                        "#${String.format(Locale.US, "%05d", t.id)}",
                        t.customerName.ifBlank { "—" },
                        t.itemsJson,
                        money(t.total),
                        t.paymentMethod,
                        t.status,
                        SimpleDateFormat("h:mm a", Locale.US).format(Date(t.createdAt))
                    )
                )
            }
        }

        w.finish()
        return toBytes(doc)
    }

    // ═══════════════════════════════════════════════════════════════
    // Sharing
    // ═══════════════════════════════════════════════════════════════

    /** Writes the PDF to cache and opens the system share sheet. */
    fun sharePdf(context: Context, fileName: String, bytes: ByteArray) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Export PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private fun money(v: Double): String = "₱" + String.format(Locale.US, "%,.2f", v)

    private fun now(): String = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date())

    private fun toBytes(doc: PdfDocument): ByteArray {
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}

// ════════════════════════════════════════════════════════════════════
// Simple table column definition
// ════════════════════════════════════════════════════════════════════

private data class Col(val header: String, val x: Float, val width: Float) {
    val maxChars: Int get() = (width / 5.2f).toInt().coerceAtLeast(4)
}

// ════════════════════════════════════════════════════════════════════
// Minimal page/table writer on top of PdfDocument
// ════════════════════════════════════════════════════════════════════

private class PdfWriter(private val doc: PdfDocument) {

    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var pageNumber = 0
    private var y = PDF_MARGIN
    private var activeTable: List<Col>? = null

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111111.toInt(); textSize = 20f; typeface = Typeface.DEFAULT_BOLD
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF666666.toInt(); textSize = 10f
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111111.toInt(); textSize = 12f; typeface = Typeface.DEFAULT_BOLD
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF222222.toInt(); textSize = 9.5f
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111111.toInt(); textSize = 9f; typeface = Typeface.DEFAULT_BOLD
    }
    private val kvLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF555555.toInt(); textSize = 10f
    }
    private val kvValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111111.toInt(); textSize = 10f; typeface = Typeface.DEFAULT_BOLD
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt(); strokeWidth = 0.6f
    }

    init { newPage() }

    fun header(title: String, subtitle: String, generated: String) {
        canvas!!.drawText(title, PDF_MARGIN, y + 16f, titlePaint)
        y += 24f
        canvas!!.drawText(subtitle, PDF_MARGIN, y, subPaint)
        y += 14f
        canvas!!.drawText(generated, PDF_MARGIN, y, subPaint)
        y += 10f
        canvas!!.drawLine(PDF_MARGIN, y, PDF_RIGHT, y, linePaint)
        y += 14f
    }

    fun section(text: String) {
        activeTable = null
        ensureSpace(24f)
        canvas!!.drawText(text, PDF_MARGIN, y + 10f, sectionPaint)
        y += 18f
    }

    fun kv(label: String, value: String) {
        ensureSpace(16f)
        canvas!!.drawText(label, PDF_MARGIN, y + 9f, kvLabelPaint)
        canvas!!.drawText(value, PDF_MARGIN + 140f, y + 9f, kvValuePaint)
        y += 15f
    }

    fun spacer(h: Float) {
        ensureSpace(h)
        y += h
    }

    fun tableHeader(cols: List<Col>) {
        ensureSpace(20f)
        activeTable = cols
        drawTableHeader(cols)
    }

    private fun drawTableHeader(cols: List<Col>) {
        canvas!!.drawLine(PDF_MARGIN, y, PDF_RIGHT, y, linePaint)
        y += 13f
        cols.forEach { c -> canvas!!.drawText(c.header, c.x, y, headerPaint) }
        y += 6f
        canvas!!.drawLine(PDF_MARGIN, y, PDF_RIGHT, y, linePaint)
        y += 10f
    }

    fun note(text: String) {
        ensureSpace(16f)
        canvas!!.drawText(text, PDF_MARGIN, y + 9f, bodyPaint)
        y += 14f
    }

    fun tableRow(cols: List<Col>, values: List<String>) {
        ensureSpace(15f)
        cols.forEachIndexed { i, c ->
            val text = if (i < values.size) values[i] else ""
            canvas!!.drawText(truncate(text, c.maxChars), c.x, y, bodyPaint)
        }
        y += 13f
    }

    fun finish() {
        if (canvas != null) doc.finishPage(page)
    }

    private fun ensureSpace(needed: Float) {
        if (y + needed > PDF_BOTTOM) newPage()
    }

    private fun newPage() {
        if (canvas != null) doc.finishPage(page)
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(PDF_PAGE_W, PDF_PAGE_H, pageNumber).create())
        canvas = page!!.canvas
        y = PDF_MARGIN
        // Repeat the current table's header when a table spans multiple pages
        activeTable?.let { drawTableHeader(it) }
    }

    private fun truncate(text: String, maxChars: Int): String {
        if (maxChars <= 0 || text.length <= maxChars) return text
        var end = maxChars - 1
        if (end > 0 && Character.isHighSurrogate(text[end - 1])) end -= 1
        return text.substring(0, end) + "…"
    }
}
