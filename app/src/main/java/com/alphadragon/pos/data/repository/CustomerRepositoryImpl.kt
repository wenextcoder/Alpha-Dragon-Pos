package com.alphadragon.pos.data.repository

import com.alphadragon.pos.data.local.dao.CustomerDao
import com.alphadragon.pos.data.local.entity.CustomerEntity
import com.alphadragon.pos.domain.model.Customer
import com.alphadragon.pos.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override fun observeAll(): Flow<List<Customer>> =
        customerDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Customer? =
        withContext(Dispatchers.IO) { customerDao.getById(id)?.toDomain() }

    override suspend fun insert(customer: Customer): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { customerDao.insert(customer.toEntity()) } }

    override suspend fun update(customer: Customer): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { customerDao.update(customer.toEntity()) } }

    override suspend fun delete(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { customerDao.delete(id) } }
}

private fun CustomerEntity.toDomain() = Customer(
    id = id,
    name = name,
    phone = phone,
    email = email,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Customer.toEntity() = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    email = email,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)
