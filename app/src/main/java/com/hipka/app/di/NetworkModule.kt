package com.hipka.app.di

import com.hipka.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

/**
 * Every table in the app goes through Retrofit (see AppModule/SongApi and
 * SocialModule/UserApi etc.) to match the networking approach Person 1
 * already established. Retrofit can't do WebSocket subscriptions, though, so
 * this SupabaseClient exists for exactly one purpose: the Realtime feed
 * ChatRepositoryImpl uses to satisfy the "DM must be WebSocket, no polling"
 * requirement. Only the Realtime plugin is installed on purpose — no
 * Postgrest here, so there's exactly one way to do a normal REST call in
 * this codebase.
 *
 * NOTE: supabase-kt's Realtime builder API has shifted a little across
 * releases (mainly how you express the per-channel filter). What
 * ChatRepositoryImpl uses matches the mainstream 2.x shape — check that
 * version's Realtime docs if `filter =` / `postgresChangeFlow` don't resolve.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Realtime)
    }
}