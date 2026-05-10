package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun observeActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category_id = :categoryId AND is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun observeByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE is_active = 1 AND (
            name LIKE '%' || :query || '%' OR 
            sku LIKE '%' || :query || '%' OR
            barcode LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
    """)
    fun search(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode AND is_active = 1 LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE track_stock = 1 AND stock_qty <= low_stock_alert AND is_active = 1")
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET stock_qty = stock_qty - :qty WHERE id = :id")
    suspend fun decrementStock(id: String, qty: Int)

    @Query("UPDATE products SET stock_qty = stock_qty + :qty WHERE id = :id")
    suspend fun incrementStock(id: String, qty: Int)

    @Query("UPDATE products SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Delete
    suspend fun delete(product: ProductEntity)
}
