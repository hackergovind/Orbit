package com.bmtp.app.di

import com.bmtp.app.mesh.ControlledFloodingPolicy
import com.bmtp.app.mesh.ForwardingPolicy
import com.bmtp.app.mesh.MeshConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeshBindingModule {

    @Binds
    @Singleton
    abstract fun bindForwardingPolicy(
        controlledFloodingPolicy: ControlledFloodingPolicy
    ): ForwardingPolicy

}

@Module
@InstallIn(SingletonComponent::class)
object MeshModule {

    @Provides
    @Singleton
    fun provideMeshConfig(): MeshConfig {
        return MeshConfig(
            maxHopCount = 15u,
            forwardQueueSize = 500,
            neighborTimeoutMs = 30_000L,
            packetExpirationMs = 60_000L,
            retryDelayMs = 5000L,
            relayDelayMs = 50L
        )
    }
}
