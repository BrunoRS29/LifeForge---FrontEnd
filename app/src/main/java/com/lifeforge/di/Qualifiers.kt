package com.lifeforge.di

import javax.inject.Qualifier

/**
 * Qualifiers para distinguir dispatchers injetados via Hilt.
 *
 * Padrão recomendado pelo Google em vez de injetar `Dispatchers.IO`
 * diretamente — permite substituir por `UnconfinedTestDispatcher` ou
 * `StandardTestDispatcher` em testes sem hack.
 */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
