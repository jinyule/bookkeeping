package com.example.bookkeeping

import android.app.Application
import androidx.room.Room
import com.example.bookkeeping.data.BookkeepingDatabase
import com.example.bookkeeping.data.CategoryEntity
import com.example.bookkeeping.data.SourceSettingEntity
import java.util.concurrent.Executors

class BookkeepingApplication : Application() {
    lateinit var database: BookkeepingDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, BookkeepingDatabase::class.java, "bookkeeping.db")
            .addMigrations(BookkeepingDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(false)
            .build()
        seedLocalDefinitions()
    }

    private fun seedLocalDefinitions() {
        Executors.newSingleThreadExecutor().execute {
            CategoryCatalog.all.forEach { def ->
                val entity = CategoryEntity()
                entity.categoryKey = def.key
                entity.direction = def.direction
                entity.labelResName = def.labelResName
                entity.sortOrder = def.order
                entity.builtIn = true
                entity.enabled = true
                database.categoryDao().insert(entity)
            }
            listOf(
                SourceKey.WECHAT,
                SourceKey.ALIPAY,
                SourceKey.UNIONPAY,
                SourceKey.VIVO_WALLET,
                SourceKey.BANK,
                SourceKey.GENERIC,
                SourceKey.IMPORT
            ).forEach { source ->
                val setting = SourceSettingEntity()
                setting.sourceKey = source
                setting.notificationEnabled = source != SourceKey.IMPORT
                setting.accessibilityEnabled = source != SourceKey.IMPORT
                setting.importEnabled = true
                setting.paused = false
                setting.updatedAt = System.currentTimeMillis()
                database.sourceSettingDao().insert(setting)
            }
        }
    }

    companion object {
        lateinit var instance: BookkeepingApplication
            private set
    }
}
