package com.lifeforge

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Classe Application do LifeForge.
 *
 * - Anotada com [HiltAndroidApp] para gerar o componente raiz do Hilt
 *   que será compartilhado por todo o ciclo de vida da aplicação.
 * - Inicializa logging via Timber (apenas em builds DEBUG).
 *
 * - Fornece ao WorkManager a fábrica de workers do Hilt ([HiltWorkerFactory]),
 *   para que o `SyncWorker` receba suas dependências por injeção. A
 *   inicialização automática do WorkManager é removida no manifesto.
 *
 * Inicializações pesadas (sincronização, pré-cache, etc.) NÃO devem ser
 * feitas aqui — a sincronização roda no WorkManager, agendada pela sessão.
 */
@HiltAndroidApp
class LifeForgeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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
