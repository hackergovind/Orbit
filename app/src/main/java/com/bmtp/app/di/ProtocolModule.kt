package com.bmtp.app.di

import com.bmtp.app.protocol.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing dependencies related to the BMTP Protocol Layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProtocolModule {

    @Binds
    @Singleton
    abstract fun bindPacketSerializer(
        impl: PacketSerializerImpl
    ): PacketSerializer

    @Binds
    @Singleton
    abstract fun bindPacketParser(
        impl: PacketParserImpl
    ): PacketParser

    @Binds
    @Singleton
    abstract fun bindPacketValidator(
        impl: PacketValidatorImpl
    ): PacketValidator

    @Binds
    @Singleton
    abstract fun bindPacketFactory(
        impl: PacketFactoryImpl
    ): PacketFactory

    companion object {
        @Provides
        @Singleton
        fun providePacketCache(): PacketCache {
            // Instantiate with constants from ProtocolConstants
            return PacketCacheImpl(
                maxSize = ProtocolConstants.CACHE_MAX_SIZE,
                expiryMs = ProtocolConstants.CACHE_EXPIRY_MS
            )
        }
    }
}
