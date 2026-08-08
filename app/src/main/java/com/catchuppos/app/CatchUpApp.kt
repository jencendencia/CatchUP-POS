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
    }

    /**
     * Start the KDS WebSocket server on the specified port
     */
    fun startKdsServer(port: Int) {
        stopKdsServer()
        try {
            val server = KdsWebSocketServer(port)

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
            kdsSettingsManager.port = port

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
            kdsServer?.stop(1000)
            kdsServer = null
            kdsSettingsManager.isEnabled = false
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

    fun closeDatabase() {
        stopKdsServer()
        AppDatabase.closeInstance()
    }
}
