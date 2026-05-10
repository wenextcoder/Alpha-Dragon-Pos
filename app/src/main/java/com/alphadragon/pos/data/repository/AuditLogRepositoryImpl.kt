package com.alphadragon.pos.data.repository

import com.alphadragon.pos.data.local.dao.AuditLogDao
import com.alphadragon.pos.data.local.entity.AuditLogEntity
import com.alphadragon.pos.domain.model.AuditLogEntry
import com.alphadragon.pos.domain.repository.AuditLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuditLogRepositoryImpl @Inject constructor(
    private val dao: AuditLogDao
) : AuditLogRepository {

    override fun observeAll(): Flow<List<AuditLogEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun log(entry: AuditLogEntry): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { dao.insert(entry.toEntity()) }
        }

    override suspend fun clearLog(clearEntryId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { dao.clearExcept(clearEntryId) }
        }

    private fun AuditLogEntity.toDomain() = AuditLogEntry(id = id, action = action, detail = detail, timestamp = timestamp)
    private fun AuditLogEntry.toEntity() = AuditLogEntity(id = id, action = action, detail = detail, timestamp = timestamp)
}
