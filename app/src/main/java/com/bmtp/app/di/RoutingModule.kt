package com.bmtp.app.di

import com.bmtp.app.routing.RoutingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoutingModule {

    @Provides
    @Singleton
    fun provideRoutingConfig(): RoutingConfig {
        return RoutingConfig(
            maxHopCount = 15u,
            routeLifetimeMs = 300_000L,
            expirySweepIntervalMs = 10_000L,
            maxRoutes = 1000,
            cacheSize = 100,
            discoveryTimeoutMs = 5000L,
            discoveryRetryCount = 3,
            repairTimeoutMs = 2000L,
            rssiThreshold = -85,
            latencyThresholdMs = 1000L,
            helloIntervalMs = 10_000L
        )
    }
}
