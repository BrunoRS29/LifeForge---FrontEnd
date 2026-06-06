package com.lifeforge.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lifeforge.BuildConfig
import com.lifeforge.data.api.AssetApi
import com.lifeforge.data.api.AuthApi
import com.lifeforge.data.api.ExpenseApi
import com.lifeforge.data.api.FinanceImportApi
import com.lifeforge.data.api.GoalApi
import com.lifeforge.data.api.IncomeApi
import com.lifeforge.data.api.OptimizationApi
import com.lifeforge.data.api.SimulationApi
import com.lifeforge.data.api.UserApi
import com.lifeforge.data.auth.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.lifeforge.data.api.PredictionApi

/**
 * Configura o stack de rede:
 *
 * - [Json] tolerante a campos desconhecidos (evita quebra quando o backend
 *   adiciona campos novos sem versionar o cliente).
 * - [OkHttpClient] com [AuthInterceptor] (Bearer token) e logging em DEBUG.
 *   Timeout de 60s para acomodar simulações Monte Carlo de 10k iterações.
 * - [Retrofit] com base URL vinda do BuildConfig (debug aponta pro emulador,
 *   release aponta pro domínio público).
 *
 * Cada API interface é exposta como um @Provides singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DEFAULT_TIMEOUT_SECONDS = 60L
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true       // não falhar quando backend adiciona campos
        coerceInputValues = true       // null em campo não-nullable vira default
        explicitNulls = false          // não emitir null em serialização
        encodeDefaults = false         // não enviar campos com valor default
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    // ========================================================================
    // APIs — uma por agregado
    // ========================================================================

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides @Singleton
    fun provideGoalApi(retrofit: Retrofit): GoalApi = retrofit.create(GoalApi::class.java)

    @Provides @Singleton
    fun provideIncomeApi(retrofit: Retrofit): IncomeApi = retrofit.create(IncomeApi::class.java)

    @Provides @Singleton
    fun provideExpenseApi(retrofit: Retrofit): ExpenseApi = retrofit.create(ExpenseApi::class.java)

    @Provides @Singleton
    fun provideAssetApi(retrofit: Retrofit): AssetApi = retrofit.create(AssetApi::class.java)

    @Provides @Singleton
    fun provideSimulationApi(retrofit: Retrofit): SimulationApi =
        retrofit.create(SimulationApi::class.java)

    @Provides @Singleton
    fun provideOptimizationApi(retrofit: Retrofit): OptimizationApi =
        retrofit.create(OptimizationApi::class.java)

    @Provides @Singleton
    fun providePredictionApi(retrofit: Retrofit): PredictionApi =
        retrofit.create(PredictionApi::class.java)

    @Provides @Singleton
    fun provideFinanceImportApi(retrofit: Retrofit): FinanceImportApi =
        retrofit.create(FinanceImportApi::class.java)
}
