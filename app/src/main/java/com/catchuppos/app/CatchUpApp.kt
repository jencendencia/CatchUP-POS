package com.catchuppos.app

import android.app.Application
import android.util.Log
import com.catchuppos.app.data.AppDatabase
import com.catchuppos.app.data.ProductRepository
import com.catchuppos.app.data.UserRepository
import com.catchuppos.app.network.KdsNsdHelper
import com.catchuppos.app.network.KdsOrderSerializer
import com.catchuppos.app.network.KdsSettingsManager
import com.catchuppos.app.network.KdsWebSocketServer
import com.catchuppos.app.network.ZeroTierManager
import com.catchuppos.app.network.ZeroTierState
import com.catchuppos.app.service.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class CatchUpApp : Application() {

    companion object {
        private const val TAG = "CatchUpApp"
    }

    // Not using by lazy so that closeInstance() + subsequent access creates a fresh DB
    val database: AppDatabase
        get() = AppDatabase.getInstance(this)
    // Also not lazy so that after close+restore, we get fresh DAOs from the new database instance
    val productRepository: ProductRepository
        get() = ProductRepository(database.productDao(), database.categoryDao(), database.transactionDao(), database.productVariantDao(), database.orderItemDao(), database.expenseDao())
    val userRepository: UserRepository
        get() = UserRepository(database.userDao())

    // KDS WebSocket server settings and server instance
    val kdsSettingsManager: KdsSettingsManager by lazy { KdsSettingsManager(this) }
    val kdsNsdHelper: KdsNsdHelper by lazy { KdsNsdHelper(this) }
    var kdsServer: KdsWebSocketServer? = null
        private set
    private var httpDashboard: com.catchuppos.app.network.HttpDashboardServer? = null

    // ZeroTier embedded VPN manager
    val zeroTierManager: ZeroTierManager by lazy { ZeroTierManager(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Seed admin synchronously so login works immediately
        runBlocking {
            try {
                userRepository.seedDefaultAdmin()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed admin: ${e.message}", e)
            }
        }
        // Seed sample data asynchronously
        applicationScope.launch {
            try {
                productRepository.seedSampleData()
            } catch (_: Exception) {}
            try {
                // Fix legacy products where Add-Ons/Merchandise items were stored as type FOOD
                productRepository.repairProductCategoryTypes()
            } catch (_: Exception) {}
        }
        // Auto-start KDS server if it was enabled before
        applicationScope.launch {
            try {
                if (kdsSettingsManager.isEnabled) {
                    startKdsServer(kdsSettingsManager.port)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-start KDS server: ${e.message}", e)
            }
        }

        // Auto-connect ZeroTier if configured
        applicationScope.launch {
            try {
                val ztSettings = zeroTierManager.getSettings()
                if (ztSettings.autoConnect && ztSettings.isNetworkConfigured()) {
                    Log.i(TAG, "Auto-connecting ZeroTier to network: ${ztSettings.networkId}")
                    connectZeroTier(ztSettings.networkId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-connect ZeroTier: ${e.message}")
            }
        }

        // When ZeroTier IP is detected, auto-start/restart KDS server on that IP
        zeroTierManager.onIpAssigned = { ip ->
            applicationScope.launch {
                Log.i(TAG, "ZeroTier IP detected: $ip — auto-starting KDS server")
                // Always start KDS on ZeroTier IP so remote access works
                startKdsServer(kdsSettingsManager.port, ip)
            }
        }
    }

    /**
     * Start the KDS WebSocket server on the specified port.
     * Binds to ZeroTier virtual IP if connected, otherwise all interfaces.
     *
     * @param port Port number to listen on
     * @param bindAddress Optional specific address to bind to. If null, uses ZeroTier IP or 0.0.0.0
     */
    fun startKdsServer(port: Int, bindAddress: String? = null) {
        stopKdsServer()
        try {
            // Determine bind address: explicit > ZeroTier IP > all interfaces
            val effectiveBind = bindAddress
                ?: getZeroTierIp()
                ?: "0.0.0.0"
            Log.i(TAG, "Starting KDS server on $effectiveBind:$port")

            val server = KdsWebSocketServer(port, effectiveBind)

            // Wire up the callback for order status updates from KDS clients
            server.onOrderStatusUpdate = { orderId, newStatus, terminalId ->
                applicationScope.launch {
                    handleKdsStatusUpdate(orderId, newStatus, terminalId)
                }
            }

            // Wire up the callback for sync requests from KDS clients
            server.onSyncRequest = { conn, terminalId ->
                applicationScope.launch {
                    handleKdsSyncRequest(server, conn, terminalId)
                }
            }

            server.start()
            kdsServer = server
            kdsSettingsManager.isEnabled = true

            // Start HTTP dashboard on port+1 for browser access
            try {
                httpDashboard?.stop()
                httpDashboard = com.catchuppos.app.network.HttpDashboardServer(
                    bindAddress = effectiveBind,
                    httpPort = port + 1
                ) {
                    kotlinx.coroutines.runBlocking {
                        buildDashboardData(effectiveBind, port + 1, port)
                    }
                }
                httpDashboard?.start()
                // Verify the server actually started
                Thread.sleep(500) // Give the thread a moment to bind
                if (httpDashboard?.isRunning() == true) {
                    Log.i(TAG, "HTTP dashboard started on $effectiveBind:${port + 1}")
                } else {
                    Log.e(TAG, "HTTP dashboard thread died immediately on $effectiveBind:${port + 1}")
                    // Try fallback: bind to all interfaces
                    if (effectiveBind != "0.0.0.0") {
                        Log.i(TAG, "Retrying HTTP dashboard on 0.0.0.0:${port + 1}")
                        httpDashboard?.stop()
                        httpDashboard = com.catchuppos.app.network.HttpDashboardServer(
                            bindAddress = "0.0.0.0",
                            httpPort = port + 1
                        ) {
                            kotlinx.coroutines.runBlocking {
                                buildDashboardData("0.0.0.0", port + 1, port)
                            }
                        }
                        httpDashboard?.start()
                        Thread.sleep(500)
                        Log.i(TAG, "HTTP dashboard retry result: running=${httpDashboard?.isRunning()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start HTTP dashboard: ${e.message}")
            }
            kdsSettingsManager.port = port

            // Start foreground service to keep servers alive when app is backgrounded
            NetworkService.start(this@CatchUpApp)

            // Register NSD service so companion apps can discover this POS automatically
            kdsNsdHelper.registerService(port)

            Log.d(TAG, "KDS server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start KDS server: ${e.message}", e)
            kdsServer = null
            kdsSettingsManager.isEnabled = false
        }
    }

    /**
     * Handle an order status update received from a KDS companion client.
     * The KDS sends the order_id in the format "ORD-{transactionId}".
     * We strip the "ORD-" prefix and update the transaction status in the database.
     */
    private suspend fun handleKdsStatusUpdate(orderId: String, newStatus: String, terminalId: String) {
        // Strip "ORD-" prefix to get the raw transactionId string
        val transactionId = orderId.removePrefix("ORD-")
        if (transactionId.isEmpty()) {
            Log.e(TAG, "Invalid order_id format: $orderId")
            return
        }

        // Look up the transaction by its custom transactionId string
        val transaction = productRepository.getTransactionByTransactionId(transactionId)
        if (transaction == null) {
            Log.e(TAG, "No transaction found with transactionId: $transactionId")
            return
        }

        // Map KDS status values to POS status strings
        val posStatus = mapKdsStatusToPos(newStatus)
        if (posStatus == null) {
            Log.e(TAG, "Unknown KDS status: $newStatus")
            return
        }

        Log.d(TAG, "Updating transaction #${transaction.id} status to: $posStatus (from KDS: $newStatus)")
        productRepository.updateTransactionStatus(transaction.id, posStatus)
    }

    /**
     * Handle a sync request from a KDS client.
     * Queries all active (non-completed) transactions from the database,
     * serializes them to the KDS JSON format using [KdsOrderSerializer],
     * and sends the orders_sync response back to the requesting client only.
     */
    private suspend fun handleKdsSyncRequest(
        server: KdsWebSocketServer,
        conn: org.java_websocket.WebSocket,
        terminalId: String
    ) {
        try {
            val activeOrders = productRepository.getActiveTransactionsWithItems()
            Log.d(TAG, "Processing sync request: ${activeOrders.size} active orders for terminal=$terminalId")

            val ordersArray = JSONArray()
            for ((transaction, items) in activeOrders) {
                val orderJson = KdsOrderSerializer.serialize(
                    transaction = transaction,
                    orderItems = items,
                    terminalId = kdsSettingsManager.getLocalIpAddress(this)
                )
                // Parse the serialized string back to JSONObject and add to the array
                ordersArray.put(JSONObject(orderJson))
            }

            val syncResponse = JSONObject().apply {
                put("type", "orders_sync")
                put("orders", ordersArray)
            }.toString()

            Log.d(TAG, "Sending orders_sync with ${activeOrders.size} orders to terminal=$terminalId")
            server.sendToClient(conn, syncResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle KDS sync request: ${e.message}", e)
        }
    }

    /**
     * Map KDS companion status strings to POS database status strings.
     * Returns null if the status is not recognized.
     */
    private fun mapKdsStatusToPos(kdsStatus: String): String? {
        return when (kdsStatus.uppercase()) {
            "PENDING" -> "Preparing"
            "IN_PROGRESS" -> "Preparing"
            "READY" -> "Ready"
            "COMPLETED" -> "Completed"
            else -> null
        }
    }

    /**
     * Stop the KDS WebSocket server
     */
    fun stopKdsServer() {
        // Unregister NSD service so companion apps stop seeing this POS
        kdsNsdHelper.unregisterService()

        try {
            httpDashboard?.stop()
            httpDashboard = null
            kdsServer?.stop(1000)
            kdsServer = null
            kdsSettingsManager.isEnabled = false
            NetworkService.stop(this)
            Log.d(TAG, "KDS server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping KDS server: ${e.message}", e)
        }
    }

    /**
     * Check if KDS server is currently running
     */
    fun isKdsServerRunning(): Boolean {
        return kdsServer != null && kdsServer!!.hasConnectedClients()
    }

    /**
     * Broadcast an order to all connected KDS clients
     */
    fun broadcastToKds(orderJson: String) {
        kdsServer?.broadcastOrder(orderJson)
    }

    // ── ZeroTier Remote Access ────────────────────────────────────

    /**
     * Connect to ZeroTier network and join it.
     * On success, the POS becomes reachable at its virtual IP.
     *
     * @param networkId ZeroTier network ID (16 hex characters)
     */
    fun connectZeroTier(networkId: String) {
        applicationScope.launch {
            try {
                zeroTierManager.start(networkId)
                Log.i(TAG, "ZeroTier connect initiated for network: $networkId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect ZeroTier: ${e.message}")
            }
        }
    }

    /**
     * Disconnect from ZeroTier
     */
    fun disconnectZeroTier() {
        zeroTierManager.stop()
    }

    /**
     * Get the current ZeroTier connection status
     */
    fun getZeroTierState(): ZeroTierState {
        return zeroTierManager.getState()
    }

    /**
     * Get the virtual IP address assigned by ZeroTier
     */
    fun getZeroTierIp(): String? {
        return zeroTierManager.getAssignedIp()
    }

    /**
     * Build dashboard data from the database. Called on each HTTP request.
     */
    private suspend fun buildDashboardData(ztIp: String, httpPort: Int, wsPort: Int): com.catchuppos.app.network.DashboardData {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 86_400_000

        val todaySales = productRepository.getTodaySales()
        val todayOrders = productRepository.getTodayOrdersCount()
        val todayItemsSold = productRepository.getTodayItemsSold()
        val totalSales = productRepository.getTotalSales()
        val totalOrders = productRepository.getTransactionCount()
        val productCount = productRepository.getProductCount()

        // Recent transactions (last 20)
        val allTxns = productRepository.getAllTransactionsOnce().take(20)
        val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.US)
        val recentTransactions = allTxns.map {
            com.catchuppos.app.network.TransactionRow(
                id = it.id,
                customerName = it.customerName,
                orderType = it.orderType,
                total = it.total,
                paymentMethod = it.paymentMethod,
                status = it.status,
                itemCount = it.itemCount,
                createdAt = sdf.format(java.util.Date(it.createdAt))
            )
        }

        // Products by category
        val allProducts = productRepository.allProductsOnce()
        val productsByCategory = allProducts
            .filter { it.isActive }
            .groupBy { it.category }
            .mapValues { (_, prods) ->
                prods.map {
                    com.catchuppos.app.network.ProductRow(
                        id = it.id,
                        title = it.title,
                        price = it.sellingPrice,
                        temperature = it.temperature,
                        stock = it.quantity,
                        isActive = it.isActive
                    )
                }
            }

        // Top selling products
        val topProducts = try {
            productRepository.getTopSellingProducts(startOfDay, endOfDay).take(5).map {
                com.catchuppos.app.network.TopProductRow(
                    name = it.productName,
                    orderCount = it.totalQty,
                    totalRevenue = it.totalSales
                )
            }
        } catch (_: Exception) { emptyList() }

        // Payment methods
        val paymentMethods = try {
            productRepository.getSalesByPaymentMethod(startOfDay, endOfDay).map {
                com.catchuppos.app.network.PaymentRow(method = it.method, totalSales = it.totalSales)
            }
        } catch (_: Exception) { emptyList() }

        // Order statuses
        val orderStatuses = try {
            productRepository.getOrderStatusCounts(startOfDay, endOfDay).map {
                com.catchuppos.app.network.StatusRow(status = it.status, count = it.count)
            }
        } catch (_: Exception) { emptyList() }

        return com.catchuppos.app.network.DashboardData(
            todaySales = todaySales,
            todayOrders = todayOrders,
            todayItemsSold = todayItemsSold,
            totalSales = totalSales,
            totalOrders = totalOrders,
            productCount = productCount,
            recentTransactions = recentTransactions,
            productsByCategory = productsByCategory,
            topProducts = topProducts,
            paymentMethods = paymentMethods,
            orderStatuses = orderStatuses,
            ztIp = ztIp,
            httpPort = httpPort,
            wsPort = wsPort
        )
    }

    fun closeDatabase() {
        stopKdsServer()
        zeroTierManager.stop()
        AppDatabase.closeInstance()
    }
}
