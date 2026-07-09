package com.catchuppos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVariantDao {

    @Query("SELECT * FROM product_variants WHERE product_id = :productId ORDER BY sort_order ASC")
    fun getVariantsByProductId(productId: Int): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants WHERE product_id = :productId ORDER BY sort_order ASC")
    suspend fun getVariantsByProductIdOnce(productId: Int): List<ProductVariantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<ProductVariantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity): Long

    @Update
    suspend fun updateVariant(variant: ProductVariantEntity)

    @Query("DELETE FROM product_variants WHERE product_id = :productId")
    suspend fun deleteVariantsByProductId(productId: Int)

    @Query("DELETE FROM product_variants WHERE id = :id")
    suspend fun deleteVariantById(id: Int)

    @Query("SELECT COUNT(*) FROM product_variants WHERE product_id = :productId")
    suspend fun getVariantCount(productId: Int): Int
}
