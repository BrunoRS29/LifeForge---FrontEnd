package com.lifeforge

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Classe Application do LifeForge.
 *
 * - Anotada com [HiltAndroidApp] para gerar o componente raiz do Hilt
 *   que será compartilhado por todo o ciclo de vida da aplicação.
 * - Inicializa logging via Timber (apenas em builds DEBUG).
 *
 * Inicializações pesadas (sincronização, pré-cache, etc.) NÃO devem ser
 * feitas aqui — usar WorkManager ou inicializar sob demanda nos UseCases.
 */
@HiltAndroidApp
class LifeForgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Em release, plantar uma Tree que reporte erros a um serviço externo
        // (Crashlytics/Sentry) — fora do escopo da Sprint 4.
    }
}
