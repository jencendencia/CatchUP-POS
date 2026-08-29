package com.catchuppos.app.network

import android.util.Log
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data container for the POS dashboard.
 * Populated by the app layer from the Room database.
 */
data class DashboardData(
    // Summary stats
    val todaySales: Double = 0.0,
    val todayOrders: Int = 0,
    val todayItemsSold: Int = 0,
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val productCount: Int = 0,

    // Recent transactions
    val recentTransactions: List<TransactionRow> = emptyList(),

    // Products by category
    val productsByCategory: Map<String, List<ProductRow>> = emptyMap(),

    // Top selling products
    val topProducts: List<TopProductRow> = emptyList(),

    // Sales by payment method
    val paymentMethods: List<PaymentRow> = emptyList(),

    // Order status breakdown
    val orderStatuses: List<StatusRow> = emptyList(),

    // Metadata
    val ztIp: String = "",
    val wsPort: Int = 8080,
    val httpPort: Int = 8081,
    val lastUpdated: String = ""
)

data class TransactionRow(
    val id: Int,
    val customerName: String,
    val orderType: String,
    val total: Double,
    val paymentMethod: String,
    val status: String,
    val itemCount: Int,
    val createdAt: String
)

data class ProductRow(
    val id: Int,
    val title: String,
    val price: Double,
    val temperature: String,
    val stock: Int,
    val isActive: Boolean
)

data class TopProductRow(
    val name: String,
    val orderCount: Int,
    val totalRevenue: Double
)

data class PaymentRow(
    val method: String,
    val totalSales: Double
)

data class StatusRow(
    val status: String,
    val count: Int
)

/**
 * Lightweight HTTP server that serves a live POS dashboard to browsers.
 */
class HttpDashboardServer(
    private val bindAddress: String,
    private val httpPort: Int,
    private val dataProvider: () -> DashboardData
) {
    companion object {
        private const val TAG = "HttpDashboardServer"
    }

    private var serverSocket: ServerSocket? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true

        thread = Thread({
            try {
                serverSocket = ServerSocket(httpPort, 50, InetAddress.getByName(bindAddress))
                Log.i(TAG, "HTTP dashboard server started on $bindAddress:$httpPort")

                while (running) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        Thread({ handleRequest(client) }, "HttpDash-Req").start()
                    } catch (e: Exception) {
                        if (running) Log.e(TAG, "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "HTTP server failed: ${e.message}")
            }
        }, "HttpDashboardServer")

        thread?.isDaemon = true
        thread?.start()
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    private fun handleRequest(client: Socket) {
        try {
            val input = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
            val requestLine = input.readLine() ?: ""
            while (input.readLine()?.isNotEmpty() == true) {}

            if (requestLine.startsWith("GET")) {
                val data = try { dataProvider() } catch (e: Exception) {
                    Log.e(TAG, "Data provider error: ${e.message}")
                    DashboardData()
                }
                val html = renderDashboard(data)
                val body = html.toByteArray(StandardCharsets.UTF_8)
                val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${body.size}\r\n" +
                    "Connection: close\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "\r\n"
                client.getOutputStream().use { os ->
                    os.write(header.toByteArray(StandardCharsets.UTF_8))
                    os.write(body)
                    os.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request error: ${e.message}")
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun fmt(n: Double): String = if (n >= 1000) String.format("%,.0f", n) else String.format("%.2f", n)

    private fun renderDashboard(d: DashboardData): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)
        val now = sdf.format(Date())

        // Build recent transactions rows
        val txnRows = if (d.recentTransactions.isEmpty()) {
            """<tr><td colspan="6" style="text-align:center;color:#666;padding:20px;">No transactions yet</td></tr>"""
        } else {
            d.recentTransactions.joinToString("\n") { t ->
                val statusColor = when (t.status) {
                    "Completed" -> "#4CAF50"
                    "Preparing" -> "#FF9800"
                    "Ready" -> "#2196F3"
                    "Cancelled" -> "#f44336"
                    else -> "#999"
                }
                """<tr>
                    <td style="color:#999;">#${t.id}</td>
                    <td>${esc(t.customerName)}</td>
                    <td>${esc(t.orderType)}</td>
                    <td>${t.itemCount}</td>
                    <td style="color:#4CAF50;font-weight:600;">₱${fmt(t.total)}</td>
                    <td><span class="status-badge" style="background:${statusColor}20;color:${statusColor}">${esc(t.status)}</span></td>
                </tr>"""
            }
        }

        // Build products by category
        val productSections = d.productsByCategory.entries.joinToString("\n") { (cat, products) ->
            val emoji = when (cat) {
                "Coffee" -> "☕"
                "Non Coffee" -> "🧋"
                "Food" -> "🛎️"
                "Add-Ons" -> "+"
                "Merchandise" -> "🛍️"
                else -> "📦"
            }
            val prodRows = products.joinToString("\n") { p ->
                val stockColor = if (p.stock <= 0) "#f44336" else if (p.stock <= 5) "#FF9800" else "#4CAF50"
                """<tr>
                    <td>${esc(p.title)}</td>
                    <td style="color:#4CAF50;">₱${fmt(p.price)}</td>
                    <td>${esc(p.temperature)}</td>
                    <td style="color:${stockColor};font-weight:600;">${p.stock}</td>
                </tr>"""
            }
            """<div class="category-section">
                <h3>${emoji} ${esc(cat)} <span class="count">(${products.size})</span></h3>
                <table>
                    <thead><tr><th>Product</th><th>Price</th><th>Temp</th><th>Stock</th></tr></thead>
                    <tbody>${prodRows}</tbody>
                </table>
            </div>"""
        }

        // Top products
        val topRows = if (d.topProducts.isEmpty()) {
            """<tr><td colspan="3" style="text-align:center;color:#666;padding:12px;">No data yet</td></tr>"""
        } else {
            d.topProducts.joinToString("\n") { p ->
                """<tr>
                    <td>${esc(p.name)}</td>
                    <td>${p.orderCount}</td>
                    <td style="color:#4CAF50;">₱${fmt(p.totalRevenue)}</td>
                </tr>"""
            }
        }

        // Payment methods
        val payRows = if (d.paymentMethods.isEmpty()) {
            """<tr><td colspan="2" style="text-align:center;color:#666;">No data</td></tr>"""
        } else {
            d.paymentMethods.joinToString("\n") { p ->
                val icon = when {
                    p.method.contains("Cash", true) -> "💵"
                    p.method.contains("Card", true) -> "💳"
                    p.method.contains("GCash", true) || p.method.contains("Grab", true) -> "📱"
                    else -> "💰"
                }
                """<tr>
                    <td>${icon} ${esc(p.method)}</td>
                    <td style="color:#4CAF50;font-weight:600;">₱${fmt(p.totalSales)}</td>
                </tr>"""
            }
        }

        // Order statuses
        val statusRows = if (d.orderStatuses.isEmpty()) {
            """<tr><td colspan="2" style="text-align:center;color:#666;">No data</td></tr>"""
        } else {
            d.orderStatuses.joinToString("\n") { s ->
                val color = when (s.status) {
                    "Completed" -> "#4CAF50"
                    "Preparing" -> "#FF9800"
                    "Ready" -> "#2196F3"
                    "Cancelled" -> "#f44336"
                    else -> "#999"
                }
                """<tr>
                    <td><span class="status-badge" style="background:${color}20;color:${color}">${esc(s.status)}</span></td>
                    <td style="font-weight:600;">${s.count}</td>
                </tr>"""
            }
        }

        return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="30">
    <title>CatchUP POS - Live Dashboard</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0a0a0a; color: #e0e0e0; padding: 0; }
        .header { background: linear-gradient(135deg, #1a1a1a 0%, #0d0d0d 100%); padding: 24px 32px; border-bottom: 1px solid #222; display: flex; justify-content: space-between; align-items: center; }
        .header h1 { color: #FF9800; font-size: 24px; }
        .header .meta { color: #666; font-size: 12px; text-align: right; }
        .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 32px; }
        .stat-card { background: #141414; border: 1px solid #222; border-radius: 12px; padding: 20px; text-align: center; }
        .stat-value { font-size: 28px; font-weight: 700; color: #FF9800; margin: 4px 0; }
        .stat-label { color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; }
        .stat-card.green .stat-value { color: #4CAF50; }
        .stat-card.blue .stat-value { color: #2196F3; }
        .section { background: #141414; border: 1px solid #222; border-radius: 12px; margin-bottom: 24px; overflow: hidden; }
        .section-header { padding: 16px 20px; border-bottom: 1px solid #222; display: flex; justify-content: space-between; align-items: center; }
        .section-header h2 { font-size: 16px; color: #fff; }
        .section-body { padding: 0; }
        table { width: 100%; border-collapse: collapse; }
        th { text-align: left; padding: 10px 16px; font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 1px; border-bottom: 1px solid #222; background: #111; }
        td { padding: 10px 16px; border-bottom: 1px solid #1a1a1a; font-size: 13px; }
        tr:hover { background: #1a1a1a; }
        .status-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
        .count { color: #666; font-weight: normal; font-size: 14px; }
        .category-section { padding: 16px 20px; border-bottom: 1px solid #1a1a1a; }
        .category-section:last-child { border-bottom: none; }
        .category-section h3 { color: #fff; font-size: 14px; margin-bottom: 10px; }
        .category-section table { width: 100%; }
        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
        @media (max-width: 768px) { .grid-2 { grid-template-columns: 1fr; } .stats-grid { grid-template-columns: repeat(2, 1fr); } }
        .live-dot { display: inline-block; width: 8px; height: 8px; background: #4CAF50; border-radius: 50%; margin-right: 6px; animation: pulse 2s infinite; }
        @keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1>☕ CatchUP POS</h1>
            <div style="color:#888;font-size:13px;margin-top:4px;">Live Dashboard</div>
        </div>
        <div class="meta">
            <div><span class="live-dot"></span>LIVE</div>
            <div style="margin-top:4px;">${esc(d.ztIp)}:${d.httpPort}</div>
            <div>Updated: $now</div>
        </div>
    </div>

    <div class="container">
        <!-- Stats -->
        <div class="stats-grid">
            <div class="stat-card green">
                <div class="stat-label">Today's Sales</div>
                <div class="stat-value">₱${fmt(d.todaySales)}</div>
            </div>
            <div class="stat-card blue">
                <div class="stat-label">Today's Orders</div>
                <div class="stat-value">${d.todayOrders}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Items Sold Today</div>
                <div class="stat-value">${d.todayItemsSold}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Products</div>
                <div class="stat-value">${d.productCount}</div>
            </div>
            <div class="stat-card green">
                <div class="stat-label">Total Sales</div>
                <div class="stat-value">₱${fmt(d.totalSales)}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Total Orders</div>
                <div class="stat-value">${d.totalOrders}</div>
            </div>
        </div>

        <!-- Recent Transactions + Side panels -->
        <div class="grid-2" style="grid-template-columns: 2fr 1fr;">
            <!-- Recent Transactions -->
            <div class="section">
                <div class="section-header">
                    <h2>📋 Recent Transactions</h2>
                    <span style="color:#666;font-size:12px;">Last 20</span>
                </div>
                <div class="section-body">
                    <table>
                        <thead>
                            <tr><th>#</th><th>Customer</th><th>Type</th><th>Items</th><th>Total</th><th>Status</th></tr>
                        </thead>
                        <tbody>${txnRows}</tbody>
                    </table>
                </div>
            </div>

            <!-- Side panels -->
            <div>
                <!-- Payment Methods -->
                <div class="section" style="margin-bottom:24px;">
                    <div class="section-header"><h2>💰 Sales by Payment</h2></div>
                    <div class="section-body">
                        <table><tbody>${payRows}</tbody></table>
                    </div>
                </div>

                <!-- Order Status -->
                <div class="section">
                    <div class="section-header"><h2>📊 Order Status</h2></div>
                    <div class="section-body">
                        <table><tbody>${statusRows}</tbody></table>
                    </div>
                </div>
            </div>
        </div>

        <!-- Top Products -->
        <div class="section" style="margin-bottom:24px;">
            <div class="section-header"><h2>🏆 Top Selling Products</h2></div>
            <div class="section-body">
                <table>
                    <thead><tr><th>Product</th><th>Orders</th><th>Revenue</th></tr></thead>
                    <tbody>${topRows}</tbody>
                </table>
            </div>
        </div>

        <!-- Products by Category -->
        <div class="section">
            <div class="section-header"><h2>📦 Menu</h2></div>
            <div class="section-body">
                ${productSections.ifEmpty { """<div style="text-align:center;color:#666;padding:30px;">No products</div>""" }}
            </div>
        </div>
    </div>

    <div style="text-align:center;padding:20px;color:#444;font-size:11px;">
        CatchUP POS • Auto-refreshes every 30s • ZeroTier Remote Access
    </div>
</body>
</html>"""
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
