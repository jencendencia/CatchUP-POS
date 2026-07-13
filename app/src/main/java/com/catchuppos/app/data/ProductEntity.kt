package com.catchuppos.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "category")
    val category: String, // "Coffee", "Non Coffee", "Food"

    @ColumnInfo(name = "type")
    val type: String, // "DRINK" or "FOOD"

    @ColumnInfo(name = "temperature")
    val temperature: String = "HOT", // "HOT", "COLD", or "BOTH"

    @ColumnInfo(name = "selling_price")
    val sellingPrice: Double = 0.0,

    @ColumnInfo(name = "cost_price")
    val costPrice: Double? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "track_inventory")
    val trackInventory: Boolean = false,

    @ColumnInfo(name = "quantity")
    val quantity: Int = 0,

    @ColumnInfo(name = "low_stock_threshold")
    val lowStockThreshold: Int = 5,

    @ColumnInfo(name = "unit")
    val unit: String = "pcs",

    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,

    @ColumnInfo(name = "add_ons_json")
    val addOnsJson: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
