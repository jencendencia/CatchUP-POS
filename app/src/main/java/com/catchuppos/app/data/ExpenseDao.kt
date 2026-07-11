package com.catchuppos.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    suspend fun getExpensesByDateRange(startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("SELECT SUM(syrups + sauce + milk + ice + others) FROM expenses WHERE date BETWEEN :startTime AND :endTime")
    suspend fun getTotalExpensesByDateRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(syrups) as syrups, SUM(sauce) as sauce, SUM(milk) as milk, SUM(ice) as ice, SUM(others) as others FROM expenses WHERE date BETWEEN :startTime AND :endTime")
    suspend fun getExpenseCategoryTotals(startTime: Long, endTime: Long): ExpenseCategoryTotals?

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startTime AND :endTime ORDER BY date ASC")
    suspend fun getExpensesByDateRangeAsc(startTime: Long, endTime: Long): List<ExpenseEntity>

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :startTime AND :endTime")
    suspend fun getExpenseCountByDateRange(startTime: Long, endTime: Long): Int
}

data class ExpenseCategoryTotals(
    @androidx.room.ColumnInfo(name = "syrups") val syrups: Double,
    @androidx.room.ColumnInfo(name = "sauce") val sauce: Double,
    @androidx.room.ColumnInfo(name = "milk") val milk: Double,
    @androidx.room.ColumnInfo(name = "ice") val ice: Double,
    @androidx.room.ColumnInfo(name = "others") val others: Double
)
