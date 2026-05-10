package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.MerchantConfigEntity
import com.alphadragon.pos.data.local.entity.TerminalConfigEntity
import com.alphadragon.pos.data.local.entity.TaxRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantConfigDao {
    @Query("SELECT * FROM merchant_configs WHERE is_active = 1")
    fun observeActive(): Flow<List<MerchantConfigEntity>>

    @Query("SELECT * FROM merchant_configs WHERE merchant_id = :merchantId LIMIT 1")
    suspend fun getByMerchantId(merchantId: String): MerchantConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: MerchantConfigEntity)

    @Update
    suspend fun update(config: MerchantConfigEntity)

    @Delete
    suspend fun delete(config: MerchantConfigEntity)
}

@Dao
interface TerminalConfigDao {
    @Query("SELECT * FROM terminal_configs WHERE is_active = 1")
    fun observeActive(): Flow<List<TerminalConfigEntity>>

    @Query("SELECT * FROM terminal_configs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TerminalConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: TerminalConfigEntity)

    @Update
    suspend fun update(config: TerminalConfigEntity)

    @Delete
    suspend fun delete(config: TerminalConfigEntity)
}

@Dao
interface TaxRuleDao {
    @Query("SELECT * FROM tax_rules ORDER BY name ASC")
    fun observeAll(): Flow<List<TaxRuleEntity>>

    @Query("SELECT * FROM tax_rules WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): TaxRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: TaxRuleEntity)

    @Update
    suspend fun update(rule: TaxRuleEntity)

    @Delete
    suspend fun delete(rule: TaxRuleEntity)
}
