package com.progali.connect.di

import android.content.Context
import com.progali.connect.data.ble.BleScanner
import com.progali.connect.data.ble.BlufiManager
import com.progali.connect.data.repository.DeviceProvisionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideBleScanner(@ApplicationContext context: Context): BleScanner {
        return BleScanner(context)
    }

    @Provides
    @Singleton
    fun provideBlufiManager(@ApplicationContext context: Context): BlufiManager {
        return BlufiManager(context)
    }

    @Provides
    @Singleton
    fun provideDeviceProvisionRepository(
        bleScanner: BleScanner,
        blufiManager: BlufiManager
    ): DeviceProvisionRepository {
        return DeviceProvisionRepository(bleScanner, blufiManager)
    }
}
