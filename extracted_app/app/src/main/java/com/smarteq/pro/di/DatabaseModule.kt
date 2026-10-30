package com.smarteq.pro.di

import android.content.Context
import com.smarteq.pro.data.local.PresetDao
import com.smarteq.pro.data.local.SmartEqDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartEqDatabase = SmartEqDatabase.build(context)

    @Provides
    @Singleton
    fun providePresetDao(database: SmartEqDatabase): PresetDao = database.presetDao()
}
