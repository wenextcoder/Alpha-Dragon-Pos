package com.alphadragon.pos.data.repository

import com.alphadragon.pos.data.local.dao.AppConfigDao
import com.alphadragon.pos.data.local.entity.AppConfigEntity
import com.alphadragon.pos.domain.repository.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppConfigRepositoryImpl @Inject constructor(
    private val dao: AppConfigDao
) : AppConfigRepository {

    override suspend fun get(key: String): String? =
        withContext(Dispatchers.IO) { dao.get(key) }

    override fun observe(key: String): Flow<String?> = dao.observe(key)

    override suspend fun set(key: String, value: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { dao.set(AppConfigEntity(key, value)) }
        }

    override suspend fun delete(key: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { dao.delete(key) } }
}
