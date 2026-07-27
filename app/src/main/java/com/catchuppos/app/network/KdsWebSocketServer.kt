package com.catchuppos.app.network

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

/**
 * WebSocket server that accepts connections from KDS (Kitchen Display System) companions.
 * When an order is completed on the POS, it broadcasts the order JSON to all connected KDS clients.
 */
class KdsWebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {

    companion object {
        private const val TAG = "KdsWebSocketServer"
    }

    private val connectedClients = mutableSetOf<WebSocket>()

    /**
     * Number of currently connected KDS clients
     */
    val clientCount: Int get() = connectedClients.size

    /**
     * Callback when a new KDS client connects
     */
    var onClientConnected: ((clientIp: String) -> Unit)? = null

    /**
     * Callback when a KDS client disconnects
     */
    var onClientDisconnected: ((clientIp: String) -> Unit)? = null

    /**
     * Callback when an order status update is received from a KDS client.
     * Provides the raw JSON message which the app layer should parse and handle.
     */
    var onOrderStatusUpdate: ((orderId: String, newStatus: String, terminalId: String) -> Unit)? = null

    /**
     * Callback when a KDS client requests a sync of all active orders.
     * The app layer should query the database and send the orders_sync response
     * back through the provided [WebSocket] connection.
     */
    var onSyncRequest: ((conn: WebSocket, terminalId: String) -> Unit)? = null

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val clientIp = conn.remoteSocketAddress?.hostString ?: "unknown"
        connectedClients.add(conn)
        Log.d(TAG, "KDS client connected: $clientIp (total: ${connectedClients.size})")
        onClientConnected?.invoke(clientIp)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val clientIp = conn.remoteSocketAddress?.hostString ?: "unknown"
        connectedClients.remove(conn)
        Log.d(TAG, "KDS client disconnected: $clientIp (total: ${connectedClients.size})")
        onClientDisconnected?.invoke(clientIp)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d(TAG, "Message from KDS client: ${message.take(150)}")

        // Parse incoming JSON messages from KDS (e.g., order status updates)
        try {
            val jsonObject = org.json.JSONObject(message)
            val type = jsonObject.optString("type", "")

            when (type) {
                "order_status_update" -> {
                    val orderId = jsonObject.optString("order_id", "")
                    val newStatus = jsonObject.optString("new_status", "")
                    val terminalId = jsonObject.optString("terminal_id", "")

                    if (orderId.isNotEmpty() && newStatus.isNotEmpty()) {
                        Log.d(TAG, "KDS status update: order=$orderId -> $newStatus from terminal=$terminalId")
                        onOrderStatusUpdate?.invoke(orderId, newStatus, terminalId)
                    }
                }
                "sync_request" -> {
                    val terminalId = jsonObject.optString("terminal_id", "unknown")
                    Log.d(TAG, "KDS sync request from terminal=$terminalId")
                    onSyncRequest?.invoke(conn, terminalId)
                }
                else -> {
                    Log.d(TAG, "Unknown KDS message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse KDS message: ${e.message}", e)
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e(TAG, "WebSocket error: ${ex.message}", ex)
    }

    override fun onStart() {
        Log.d(TAG, "KDS WebSocket server started on port ${address.port}")
        setConnectionLostTimeout(30)
    }

    /**
     * Broadcast an order JSON to all connected KDS clients
     */
    fun broadcastOrder(orderJson: String) {
        if (connectedClients.isEmpty()) {
            Log.d(TAG, "No KDS clients connected, skipping broadcast")
            return
        }
        Log.d(TAG, "Broadcasting order to ${connectedClients.size} KDS client(s)")
        // Send as text frame (String), NOT as binary frame (byte[]) —
        // the companion app's WebSocketListener only overrides onMessage(text),
        // so binary frames would be silently discarded.
        broadcast(orderJson)
    }

    /**
     * Send a message to a specific connected KDS client.
     * Unlike broadcastOrder(), this sends only to one client.
     */
    fun sendToClient(conn: WebSocket, message: String) {
        if (conn.isOpen) {
            conn.send(message)
        } else {
            Log.w(TAG, "Cannot send to client, connection is closed")
        }
    }

    /**
     * Check if there are any connected KDS clients
     */
    fun hasConnectedClients(): Boolean = connectedClients.isNotEmpty()

    /**
     * Get list of connected client IPs
     */
    fun getConnectedClientIps(): List<String> {
        return connectedClients.mapNotNull { it.remoteSocketAddress?.hostString }
    }
}
