package com.alphadragon.pos.data.repository

import com.alphadragon.pos.data.local.dao.AdminDao
import com.alphadragon.pos.data.local.dao.AppConfigDao
import com.alphadragon.pos.data.local.entity.AdminEntity
import com.alphadragon.pos.data.local.entity.AppConfigEntity
import com.alphadragon.pos.domain.model.Admin
import com.alphadragon.pos.domain.repository.AdminRepository
import com.alphadragon.core.common.ConfigKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val adminDao: AdminDao,
    private val appConfigDao: AppConfigDao
) : AdminRepository {

    override suspend fun isSetupComplete(): Boolean = withContext(Dispatchers.IO) {
        appConfigDao.isSetupComplete() > 0
    }

    override suspend fun getAdmin(): Admin? = withContext(Dispatchers.IO) {
        adminDao.getAdmin()?.toDomain()
    }

    override suspend fun createAdmin(admin: Admin, shopName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                adminDao.insert(admin.toEntity())
                appConfigDao.set(AppConfigEntity(ConfigKeys.SHOP_NAME, shopName))
                appConfigDao.set(AppConfigEntity(ConfigKeys.SETUP_COMPLETE, "true"))
            }
        }

    override suspend fun updateLastLogin(adminId: String, timestamp: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { adminDao.updateLastLogin(adminId, timestamp) }
        }

    override suspend fun updatePinHash(adminId: String, pinHash: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { adminDao.updatePinHash(adminId, pinHash) }
        }

    private fun AdminEntity.toDomain() = Admin(
        id = id,
        username = username,
        displayName = displayName,
        pinHash = pinHash,
        createdAt = createdAt,
        lastLogin = lastLogin
    )

    private fun Admin.toEntity() = AdminEntity(
        id = id,
        username = username,
        displayName = displayName,
        pinHash = pinHash,
        createdAt = createdAt,
        lastLogin = lastLogin
    )
}
