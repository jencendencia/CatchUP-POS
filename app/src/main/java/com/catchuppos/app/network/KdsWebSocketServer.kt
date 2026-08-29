package com.catchuppos.app.network

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.java_websocket.drafts.Draft
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ServerHandshakeBuilder
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * WebSocket server that accepts connections from KDS (Kitchen Display System) companions.
 * When an order is completed on the POS, it broadcasts the order JSON to all connected KDS clients.
 *
 * Supports binding to a specific address (e.g. ZeroTier virtual IP) or all interfaces.
 */
class KdsWebSocketServer(
    port: Int,
    bindAddress: String = "0.0.0.0"
) : WebSocketServer(InetSocketAddress(bindAddress, port)) {

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
        Log.d(TAG, "KDS WebSocket server started on ${address.hostString}:${address.port}")
        setConnectionLostTimeout(30)
    }

    /**
     * Override to handle HTTP GET requests from browsers
     */
    override fun onWebsocketHandshakeReceivedAsServer(
        conn: WebSocket,
        draft: Draft,
        request: ClientHandshake
    ): ServerHandshakeBuilder {
        val builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request)
        
        // If it's a browser request (not a WebSocket upgrade)
        if (!request.hasFieldValue("Upgrade") || !request.getFieldValue("Upgrade").equals("websocket", ignoreCase = true)) {
            val path = request.resourceDescriptor
            if (path == "/" || path == "/index.html") {
                sendHttpDashboard(conn)
            }
        }
        
        return builder
    }

    private fun sendHttpDashboard(conn: WebSocket) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>CatchUP KDS Dashboard</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: sans-serif; background: #000; color: #fff; padding: 20px; text-align: center; }
                    .card { background: #1a1a1a; border-radius: 12px; padding: 20px; margin-top: 50px; border: 1px solid #333; }
                    h1 { color: #FF9800; }
                    .status { font-weight: bold; color: #4CAF50; margin: 10px 0; }
                    p { color: #999; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>CatchUP KDS</h1>
                    <div class="status">● Server is Active</div>
                    <p>ZeroTier IP: ${address.hostString}</p>
                    <p>To view active orders, please use the KDS Companion Android App or a WebSocket client.</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: ${html.toByteArray(StandardCharsets.UTF_8).size}\r\n" +
                "Connection: close\r\n\r\n" +
                html

        conn.send(response)
        conn.close(CloseFrame.NORMAL)
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
