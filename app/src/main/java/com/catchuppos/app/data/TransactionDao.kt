package com.catchuppos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    suspend fun getTransactionsByDateRange(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE status = :status")
    suspend fun getTransactionCountByStatus(status: String): Int

    @Query("SELECT SUM(total) FROM transactions")
    suspend fun getTotalSales(): Double?

    @Query("SELECT SUM(item_count) FROM transactions")
    suspend fun getTotalItemsSold(): Int?

    @Query("SELECT SUM(total) FROM transactions WHERE created_at >= :startOfDay")
    suspend fun getTodaySales(startOfDay: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at >= :startOfDay")
    suspend fun getTodayCustomersServed(startOfDay: Long): Int

    @Query("SELECT * FROM transactions WHERE status IN (:statuses) ORDER BY created_at DESC")
    suspend fun getTransactionsByStatuses(statuses: List<String>): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getTransactionByTransactionId(transactionId: String): TransactionEntity?

    @Query("UPDATE transactions SET status = :newStatus WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: Int, newStatus: String)

    @Query("SELECT payment_method as method, SUM(total) as total_sales FROM transactions WHERE created_at BETWEEN :startTime AND :endTime GROUP BY payment_method ORDER BY total_sales DESC")
    suspend fun getSalesByPaymentMethod(startTime: Long, endTime: Long): List<PaymentMethodSales>

    @Query("SELECT SUM(total) FROM transactions WHERE created_at BETWEEN :startTime AND :endTime")
    suspend fun getSalesTotalByDateRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at BETWEEN :startTime AND :endTime")
    suspend fun getOrdersCountByDateRange(startTime: Long, endTime: Long): Int

    @Query("SELECT SUM(item_count) FROM transactions WHERE created_at BETWEEN :startTime AND :endTime")
    suspend fun getItemsSoldByDateRange(startTime: Long, endTime: Long): Int?

    @Query("""
        SELECT (CAST(strftime('%H', created_at / 1000, 'unixepoch', 'localtime') AS INTEGER)) as hour, SUM(total) as amount
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY hour
        ORDER BY hour ASC
    """)
    suspend fun getHourlySales(startTime: Long, endTime: Long): List<HourlySalesSummary>

    @Query("""
        SELECT (created_at / 86400000) as day_offset, SUM(total) as total, COUNT(*) as order_count
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY day_offset
        ORDER BY day_offset ASC
    """)
    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySalesSummary>

    @Query("""
        SELECT (created_at / 86400000) as day_offset, SUM(item_count) as total_items
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY day_offset
        ORDER BY day_offset ASC
    """)
    suspend fun getDailyItemsSold(startTime: Long, endTime: Long): List<DailyItemsSold>

    @Query("""
        SELECT customer_name, COUNT(*) as order_count, SUM(total) as total_spent
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY customer_name
        ORDER BY total_spent DESC
        LIMIT :limit
    """)
    suspend fun getTopCustomers(startTime: Long, endTime: Long, limit: Int = 10): List<CustomerSummary>

    @Query("""
        SELECT cashier_name, COUNT(*) as order_count, SUM(total) as total_sales
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime AND cashier_name != ''
        GROUP BY cashier_name
        ORDER BY total_sales DESC
    """)
    suspend fun getCashierPerformance(startTime: Long, endTime: Long): List<CashierSummary>

    @Query("""
        SELECT status, COUNT(*) as count
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY status
        ORDER BY count DESC
    """)
    suspend fun getOrderStatusCounts(startTime: Long, endTime: Long): List<OrderStatusSummary>

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at BETWEEN :startTime AND :endTime AND customer_name = :customerName")
    suspend fun getCustomerOrderCount(startTime: Long, endTime: Long, customerName: String): Int

    @Query("""
        SELECT order_type as order_type, COUNT(*) as order_count, SUM(total) as total_sales
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY order_type
        ORDER BY total_sales DESC
    """)
    suspend fun getOrderTypeCounts(startTime: Long, endTime: Long): List<OrderTypeSummary>
}

data class PaymentMethodSales(
    @androidx.room.ColumnInfo(name = "method") val method: String,
    @androidx.room.ColumnInfo(name = "total_sales") val totalSales: Double
)

data class HourlySalesSummary(
    @androidx.room.ColumnInfo(name = "hour") val hour: Int,
    @androidx.room.ColumnInfo(name = "amount") val amount: Double
)

data class DailySalesSummary(
    @androidx.room.ColumnInfo(name = "day_offset") val dayOffset: Long,
    @androidx.room.ColumnInfo(name = "total") val total: Double,
    @androidx.room.ColumnInfo(name = "order_count") val orderCount: Int
)

data class DailyItemsSold(
    @androidx.room.ColumnInfo(name = "day_offset") val dayOffset: Long,
    @androidx.room.ColumnInfo(name = "total_items") val totalItems: Int
)

data class CustomerSummary(
    @androidx.room.ColumnInfo(name = "customer_name") val customerName: String,
    @androidx.room.ColumnInfo(name = "order_count") val orderCount: Int,
    @androidx.room.ColumnInfo(name = "total_spent") val totalSpent: Double
)

data class CashierSummary(
    @androidx.room.ColumnInfo(name = "cashier_name") val cashierName: String,
    @androidx.room.ColumnInfo(name = "order_count") val orderCount: Int,
    @androidx.room.ColumnInfo(name = "total_sales") val totalSales: Double
)

data class OrderStatusSummary(
    @androidx.room.ColumnInfo(name = "status") val status: String,
    @androidx.room.ColumnInfo(name = "count") val count: Int
)

data class OrderTypeSummary(
    @androidx.room.ColumnInfo(name = "order_type") val orderType: String,
    @androidx.room.ColumnInfo(name = "order_count") val orderCount: Int,
    @androidx.room.ColumnInfo(name = "total_sales") val totalSales: Double
)
