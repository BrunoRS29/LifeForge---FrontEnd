package com.lifeforge.di

import com.lifeforge.data.repository.AssetRepositoryImpl
import com.lifeforge.data.repository.AuthRepositoryImpl
import com.lifeforge.data.repository.ExpenseRepositoryImpl
import com.lifeforge.data.repository.ExpenseScheduleRepositoryImpl
import com.lifeforge.data.repository.GoalRepositoryImpl
import com.lifeforge.data.repository.IncomeRepositoryImpl
import com.lifeforge.data.repository.IncomeScheduleRepositoryImpl
import com.lifeforge.data.repository.OptimizationRepositoryImpl
import com.lifeforge.data.repository.SimulationRepositoryImpl
import com.lifeforge.data.repository.UserRepositoryImpl
import com.lifeforge.domain.repository.AssetRepository
import com.lifeforge.domain.repository.AuthRepository
import com.lifeforge.domain.repository.ExpenseRepository
import com.lifeforge.domain.repository.ExpenseScheduleRepository
import com.lifeforge.domain.repository.GoalRepository
import com.lifeforge.domain.repository.IncomeRepository
import com.lifeforge.domain.repository.IncomeScheduleRepository
import com.lifeforge.domain.repository.OptimizationRepository
import com.lifeforge.domain.repository.SimulationRepository
import com.lifeforge.domain.repository.UserRepository
import com.lifeforge.data.repository.PredictionRepositoryImpl
import com.lifeforge.domain.repository.PredictionRepository
import com.lifeforge.data.repository.StatementImportRepositoryImpl
import com.lifeforge.domain.repository.StatementImportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Liga cada interface de [com.lifeforge.domain.repository] à sua
 * implementação concreta em [com.lifeforge.data.repository].
 *
 * Usa `@Binds` (em vez de `@Provides`) porque o Hilt sabe instanciar
 * cada Impl direto via `@Inject` constructor — o module só precisa
 * dizer "esse Impl satisfaz essa interface". Mais eficiente em build
 * time que `@Provides`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindIncomeScheduleRepository(impl: IncomeScheduleRepositoryImpl): IncomeScheduleRepository

    @Binds
    @Singleton
    abstract fun bindExpenseScheduleRepository(impl: ExpenseScheduleRepositoryImpl): ExpenseScheduleRepository

    @Binds
    @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository

    @Binds
    @Singleton
    abstract fun bindSimulationRepository(impl: SimulationRepositoryImpl): SimulationRepository

    @Binds
    @Singleton
    abstract fun bindOptimizationRepository(impl: OptimizationRepositoryImpl): OptimizationRepository

    @Binds
    @Singleton
    abstract fun bindPredictionRepository(impl: PredictionRepositoryImpl): PredictionRepository

    @Binds
    @Singleton
    abstract fun bindStatementImportRepository(
        impl: StatementImportRepositoryImpl,
    ): StatementImportRepository
}
