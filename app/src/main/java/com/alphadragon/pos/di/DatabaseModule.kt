package com.alphadragon.pos.di

import android.content.Context
import androidx.room.Room
import com.alphadragon.pos.data.local.AlphaDragonDb
import com.alphadragon.pos.data.local.MIGRATION_1_2
import com.alphadragon.pos.data.local.MIGRATION_2_3
import com.alphadragon.pos.data.local.MIGRATION_3_4
import com.alphadragon.pos.security.KeystoreManager
import com.alphadragon.core.common.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): AlphaDragonDb {
        // Passphrase is randomly generated on first launch, then AES-GCM encrypted
        // and stored in SharedPreferences. Hardware-backed Keystore keys cannot expose
        // raw bytes via SecretKey.encoded — so we wrap the passphrase, not the key.
        val passphrase = keystoreManager.getOrCreateDbPassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AlphaDragonDb::class.java,
            AppConfig.DB_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration() // dev safety net: wipes and recreates on hash mismatch
            .build()
    }

    @Provides fun provideAppConfigDao(db: AlphaDragonDb) = db.appConfigDao()
    @Provides fun provideAdminDao(db: AlphaDragonDb) = db.adminDao()
    @Provides fun provideCategoryDao(db: AlphaDragonDb) = db.categoryDao()
    @Provides fun provideProductDao(db: AlphaDragonDb) = db.productDao()
    @Provides fun provideCustomerDao(db: AlphaDragonDb) = db.customerDao()
    @Provides fun provideTransactionDao(db: AlphaDragonDb) = db.transactionDao()
    @Provides fun provideTransactionItemDao(db: AlphaDragonDb) = db.transactionItemDao()
    @Provides fun provideRefundDao(db: AlphaDragonDb) = db.refundDao()
    @Provides fun provideMerchantConfigDao(db: AlphaDragonDb) = db.merchantConfigDao()
    @Provides fun provideTerminalConfigDao(db: AlphaDragonDb) = db.terminalConfigDao()
    @Provides fun provideTaxRuleDao(db: AlphaDragonDb) = db.taxRuleDao()
    @Provides fun provideAuditLogDao(db: AlphaDragonDb) = db.auditLogDao()
}
