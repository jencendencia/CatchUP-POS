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

    @Query("SELECT SUM(total) FROM transactions WHERE status = 'Completed'")
    suspend fun getTotalSales(): Double?

    @Query("SELECT SUM(item_count) FROM transactions")
    suspend fun getTotalItemsSold(): Int?

    @Query("SELECT SUM(total) FROM transactions WHERE status = 'Completed' AND created_at >= :startOfDay")
    suspend fun getTodaySales(startOfDay: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'Completed' AND created_at >= :startOfDay")
    suspend fun getTodayCustomersServed(startOfDay: Long): Int
}
