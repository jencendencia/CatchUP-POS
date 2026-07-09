package com.catchuppos.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "customer_name")
    val customerName: String = "Valued Customer",

    @ColumnInfo(name = "items_json")
    val itemsJson: String,

    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0,

    @ColumnInfo(name = "total")
    val total: Double = 0.0,

    @ColumnInfo(name = "amount_tendered")
    val amountTendered: Double = 0.0,

    @ColumnInfo(name = "change_returned")
    val changeReturned: Double = 0.0,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "Cash",

    @ColumnInfo(name = "status")
    val status: String = "Completed",

    @ColumnInfo(name = "transaction_id")
    val transactionId: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
