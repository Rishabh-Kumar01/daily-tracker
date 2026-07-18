package dev.rishabh.dailytracker.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.rishabh.dailytracker.core.network.OpenFoodFactsClient
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * The app's only outbound network surface (Phase 1).
 *
 * Deliberately small: the sole call is the Open Food Facts barcode lookup. Nothing about
 * the user is sent — a barcode is a property of the packet, not of who scanned it.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCallFactory(): Call.Factory = OkHttpClient.Builder()
        // Short timeouts: the scanner is an interactive surface, and falling through to
        // manual entry quickly beats a spinner that outlasts the user's patience.
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideOpenFoodFactsClient(callFactory: Call.Factory, json: Json): OpenFoodFactsClient =
        OpenFoodFactsClient(callFactory, json)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // OFF returns hundreds of fields per product and adds more over time; the DTOs
        // name only what is used. Unknown keys must not fail the parse.
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}
