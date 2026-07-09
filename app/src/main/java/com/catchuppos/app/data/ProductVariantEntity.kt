package com.catchuppos.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_variants",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("product_id")]
)
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "product_id")
    val productId: Int,

    @ColumnInfo(name = "size_name")
    val sizeName: String,

    @ColumnInfo(name = "selling_price")
    val sellingPrice: Double,

    @ColumnInfo(name = "cost_price")
    val costPrice: Double? = null,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
