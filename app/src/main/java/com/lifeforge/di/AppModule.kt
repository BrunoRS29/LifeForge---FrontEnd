package com.lifeforge.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Providers de baixo nível independentes de aggregate.
 *
 * [Clock] é injetado nos use cases que comparam datas (ex.: validar
 * `targetDate` no futuro) — passar pelo Hilt em vez de chamar
 * `Instant.now()` direto torna a validação determinística em testes.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
