package com.catchuppos.app.network

import com.catchuppos.app.data.OrderItemEntity
import com.catchuppos.app.data.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Serializes POS transactions into the JSON format expected by the KDS companion app.
 *
 * The KDS expects:
 * ```json
 * {
 *   "order_id": "ORD-2026-0892",
 *   "pos_terminal_id": "TERM-01",
 *   "timestamp": "2026-07-27T17:10:39Z",
 *   "payment_status": "PAID",
 *   "order_type": "TAKEOUT",
 *   "customer_name": "Joel",
 *   "items": [{
 *     "item_id": "ITEM-101",
 *     "name": "Iced Spanish Latte",
 *     "quantity": 2,
 *     "size": "Large",
 *     "modifiers": ["Oat Milk", "Less Ice"],
 *     "unit_price": 85.0
 *   }]
 * }
 * ```
 */
object KdsOrderSerializer {

    private const val TAG = "KdsOrderSerializer"

    /**
     * Convert a transaction and its items to the KDS-compatible JSON format
     */
    fun serialize(
        transaction: TransactionEntity,
        orderItems: List<OrderItemEntity>,
        terminalId: String = "TERM-01"
    ): String {
        val orderId = buildOrderId(transaction.transactionId)
        val timestamp = formatTimestamp(transaction.createdAt)
        val orderType = determineOrderType(transaction.orderType)

        return JSONObject().apply {
            put("order_id", orderId)
            put("pos_terminal_id", terminalId)
            put("timestamp", timestamp)
            put("payment_status", "PAID")
            put("order_type", orderType)
            put("customer_name", transaction.customerName)

            // Build items array
            val itemsArray = JSONArray()
            orderItems.forEach { item ->
                itemsArray.put(serializeItem(item))
            }
            put("items", itemsArray)
        }.toString()
    }

    /**
     * Serialize a single order item to KDS format
     */
    private fun serializeItem(item: OrderItemEntity): JSONObject {
        return JSONObject().apply {
            put("item_id", "ITEM-${item.productId}")
            put("name", item.productName)
            put("quantity", item.quantity)
            put("size", item.size)
            put("modifiers", JSONArray()) // Modifiers not tracked in current POS schema
            put("unit_price", item.unitPrice)
        }
    }

    /**
     * Build a human-readable order ID from the transaction ID
     */
    private fun buildOrderId(transactionId: String): String {
        // Format: ORD-YYYYMMDD-HHMMSS (or use transaction ID directly)
        if (transactionId.isNotEmpty()) {
            return "ORD-${transactionId}"
        }
        val now = Calendar.getInstance()
        return String.format(
            "ORD-%04d%02d%02d%02d%02d",
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
    }

    /**
     * Format timestamp to ISO 8601
     */
    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    /**
     * Map the POS order type to the KDS enum values
     */
    private fun determineOrderType(orderType: String): String {
        return if (orderType.contains("Dine", ignoreCase = true)) "DINE_IN" else "TAKEOUT"
    }
}
