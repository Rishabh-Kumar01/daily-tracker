package dev.rishabh.dailytracker.core.db.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.SystemTimeSource
import dev.rishabh.dailytracker.core.common.TimeSource
import dev.rishabh.dailytracker.core.common.UuidGenerator
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.dao.LogDao
import dev.rishabh.dailytracker.core.db.dao.ProductDao
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DailyTrackerDatabase =
        Room.databaseBuilder(context, DailyTrackerDatabase::class.java, DailyTrackerDatabase.NAME)
            // No fallbackToDestructiveMigration: this is the user's only copy of their
            // history and there is no cloud backup. A missing migration must fail loudly
            // in development, never silently wipe logs on a real device.
            .build()

    @Provides
    fun provideTemplateDao(db: DailyTrackerDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideLogDao(db: DailyTrackerDatabase): LogDao = db.logDao()

    @Provides
    fun provideProductDao(db: DailyTrackerDatabase): ProductDao = db.productDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {

    @Binds
    @Singleton
    abstract fun bindIdGenerator(impl: UuidGenerator): IdGenerator

    @Binds
    @Singleton
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
