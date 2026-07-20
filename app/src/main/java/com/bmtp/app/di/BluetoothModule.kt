package com.bmtp.app.di

import com.bmtp.app.bluetooth.BleAdvertiser
import com.bmtp.app.bluetooth.BleAdvertiserImpl
import com.bmtp.app.bluetooth.BlePermissionManager
import com.bmtp.app.bluetooth.BlePermissionManagerImpl
import com.bmtp.app.bluetooth.BleScanner
import com.bmtp.app.bluetooth.BleScannerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Bluetooth components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {

    @Binds
    @Singleton
    abstract fun bindBleScanner(
        impl: BleScannerImpl
    ): BleScanner

    @Binds
    @Singleton
    abstract fun bindBleAdvertiser(
        impl: BleAdvertiserImpl
    ): BleAdvertiser

    @Binds
    @Singleton
    abstract fun bindBlePermissionManager(
        impl: BlePermissionManagerImpl
    ): BlePermissionManager
}
