package com.lifeforge.di

import android.content.Context
import androidx.work.WorkManager
import com.lifeforge.data.api.AssetApi
import com.lifeforge.data.api.ExpenseApi
import com.lifeforge.data.api.GoalApi
import com.lifeforge.data.api.IncomeApi
import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.model.dto.AssetRequestDto
import com.lifeforge.data.model.dto.ExpenseRequestDto
import com.lifeforge.data.model.dto.GoalRequestDto
import com.lifeforge.data.model.dto.IncomeRequestDto
import com.lifeforge.data.preferences.AppPreferencesStore
import com.lifeforge.data.repository.SimulationRepositoryImpl
import com.lifeforge.data.repository.SyncRepositoryImpl
import com.lifeforge.data.sync.CacheRefresher
import com.lifeforge.data.sync.ConnectivityNetworkMonitor
import com.lifeforge.data.sync.EntitySyncHandler
import com.lifeforge.data.sync.LastSyncStore
import com.lifeforge.data.sync.NetworkMonitor
import com.lifeforge.data.sync.OutboxEntityType
import com.lifeforge.data.sync.RemoteEntitySyncHandler
import com.lifeforge.data.sync.RoomTransactionRunner
import com.lifeforge.data.sync.SyncScheduler
import com.lifeforge.data.sync.TransactionRunner
import com.lifeforge.data.sync.WorkManagerSyncScheduler
import com.lifeforge.data.sync.asLocalStore
import com.lifeforge.data.sync.asRemoteCrud
import com.lifeforge.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Peças da sincronização offline-first: um handler por entidade editável
 * (multibinding em `Set<EntitySyncHandler>`), atualizadores extras do cache,
 * monitor de rede, agendador (WorkManager) e o repositório exposto à UI.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds @Singleton
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner

    @Binds @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds @Singleton
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds @Singleton
    abstract fun bindLastSyncStore(impl: AppPreferencesStore): LastSyncStore

    @Binds @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    /** Histórico de simulações de todas as metas, atualizado após as entidades. */
    @Binds @IntoSet
    abstract fun bindSimulationHistoryRefresher(impl: SimulationRepositoryImpl): CacheRefresher

    companion object {

        @Provides @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)

        @Provides @IntoSet
        fun provideGoalSyncHandler(
            api: GoalApi, dao: GoalDao, json: Json, tx: TransactionRunner,
        ): EntitySyncHandler = RemoteEntitySyncHandler(
            type = OutboxEntityType.GOAL,
            store = dao.asLocalStore(),
            remote = api.asRemoteCrud(),
            requestSerializer = GoalRequestDto.serializer(),
            toEntity = { it.toEntity() },
            json = json,
            tx = tx,
        )

        @Provides @IntoSet
        fun provideIncomeSyncHandler(
            api: IncomeApi, dao: IncomeDao, json: Json, tx: TransactionRunner,
        ): EntitySyncHandler = RemoteEntitySyncHandler(
            type = OutboxEntityType.INCOME,
            store = dao.asLocalStore(),
            remote = api.asRemoteCrud(),
            requestSerializer = IncomeRequestDto.serializer(),
            toEntity = { it.toEntity() },
            json = json,
            tx = tx,
        )

        @Provides @IntoSet
        fun provideExpenseSyncHandler(
            api: ExpenseApi, dao: ExpenseDao, json: Json, tx: TransactionRunner,
        ): EntitySyncHandler = RemoteEntitySyncHandler(
            type = OutboxEntityType.EXPENSE,
            store = dao.asLocalStore(),
            remote = api.asRemoteCrud(),
            requestSerializer = ExpenseRequestDto.serializer(),
            toEntity = { it.toEntity() },
            json = json,
            tx = tx,
        )

        @Provides @IntoSet
        fun provideAssetSyncHandler(
            api: AssetApi, dao: AssetDao, json: Json, tx: TransactionRunner,
        ): EntitySyncHandler = RemoteEntitySyncHandler(
            type = OutboxEntityType.ASSET,
            store = dao.asLocalStore(),
            remote = api.asRemoteCrud(),
            requestSerializer = AssetRequestDto.serializer(),
            toEntity = { it.toEntity() },
            json = json,
            tx = tx,
        )
    }
}
