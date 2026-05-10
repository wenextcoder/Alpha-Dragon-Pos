package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY display_order ASC, name ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parent_id IS NULL AND is_active = 1 ORDER BY display_order ASC")
    fun observeTopLevel(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parent_id = :parentId AND is_active = 1 ORDER BY display_order ASC")
    fun observeSubcategories(parentId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Delete
    suspend fun delete(category: CategoryEntity)
}
