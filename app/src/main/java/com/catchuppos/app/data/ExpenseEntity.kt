package com.catchuppos.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "syrups")
    val syrups: Double = 0.0,

    @ColumnInfo(name = "sauce")
    val sauce: Double = 0.0,

    @ColumnInfo(name = "milk")
    val milk: Double = 0.0,

    @ColumnInfo(name = "ice")
    val ice: Double = 0.0,

    @ColumnInfo(name = "others")
    val others: Double = 0.0,

    @ColumnInfo(name = "vendor")
    val vendor: String = "",

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
