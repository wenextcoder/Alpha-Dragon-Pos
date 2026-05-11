package com.alphadragon.pos.di

import com.alphadragon.pos.data.repository.*
import com.alphadragon.pos.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds @Singleton
    abstract fun bindAppConfigRepository(impl: AppConfigRepositoryImpl): AppConfigRepository

    @Binds @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindAuditLogRepository(impl: AuditLogRepositoryImpl): AuditLogRepository
}
