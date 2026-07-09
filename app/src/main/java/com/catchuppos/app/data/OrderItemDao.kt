package com.catchuppos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OrderItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE transaction_id = :transactionId")
    suspend fun getOrderItemsByTransactionId(transactionId: Int): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE transaction_id = :transactionId")
    fun getOrderItemsByTransactionIdFlow(transactionId: Int): kotlinx.coroutines.flow.Flow<List<OrderItemEntity>>

    @Query("SELECT product_name, SUM(quantity) as total_qty FROM order_items GROUP BY product_name ORDER BY total_qty DESC")
    suspend fun getProductSalesSummary(): List<ProductSalesSummary>

    @Query("SELECT SUM(subtotal) FROM order_items WHERE product_name = :productName")
    suspend fun getTotalSalesByProduct(productName: String): Double?
}

data class ProductSalesSummary(
    @androidx.room.ColumnInfo(name = "product_name") val productName: String,
    @androidx.room.ColumnInfo(name = "total_qty") val totalQty: Int
)
