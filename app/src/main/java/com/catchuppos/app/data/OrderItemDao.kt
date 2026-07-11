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

    @Query("""
        SELECT p.category, SUM(oi.subtotal) as total_sales
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        JOIN transactions t ON oi.transaction_id = t.id
        WHERE t.created_at BETWEEN :startTime AND :endTime
        GROUP BY p.category
        ORDER BY total_sales DESC
    """)
    suspend fun getCategorySalesSummary(startTime: Long, endTime: Long): List<CategorySalesSummary>

    @Query("""
        SELECT oi.product_name, SUM(oi.quantity) as total_qty, SUM(oi.subtotal) as total_sales
        FROM order_items oi
        JOIN transactions t ON oi.transaction_id = t.id
        WHERE t.created_at BETWEEN :startTime AND :endTime
        GROUP BY oi.product_name
        ORDER BY total_qty DESC
        LIMIT 5
    """)
    suspend fun getTopSellingProducts(startTime: Long, endTime: Long): List<TopSellingProduct>
}

data class ProductSalesSummary(
    @androidx.room.ColumnInfo(name = "product_name") val productName: String,
    @androidx.room.ColumnInfo(name = "total_qty") val totalQty: Int
)

data class CategorySalesSummary(
    @androidx.room.ColumnInfo(name = "category") val category: String,
    @androidx.room.ColumnInfo(name = "total_sales") val totalSales: Double
)

data class TopSellingProduct(
    @androidx.room.ColumnInfo(name = "product_name") val productName: String,
    @androidx.room.ColumnInfo(name = "total_qty") val totalQty: Int,
    @androidx.room.ColumnInfo(name = "total_sales") val totalSales: Double
)
