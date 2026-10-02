package com.lifeforge.di

import android.content.Context
import androidx.room.Room
import com.lifeforge.data.db.LifeForgeDatabase
import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.db.dao.PendingOperationDao
import com.lifeforge.data.db.dao.SimulationDao
import com.lifeforge.data.db.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provê o [LifeForgeDatabase] e cada DAO como singletons injetáveis.
 *
 * [Room.databaseBuilder] é caro — daí o Singleton no @Provides do banco.
 * Os DAOs em si são leves, mas também são singletons porque são wrappers
 * em torno do banco e não têm estado próprio.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLifeForgeDatabase(
        @ApplicationContext context: Context,
    ): LifeForgeDatabase = Room.databaseBuilder(
        context,
        LifeForgeDatabase::class.java,
        LifeForgeDatabase.DATABASE_NAME,
    )
        // Desde a v2 o banco guarda alterações feitas sem conexão (fila de
        // saída): cada mudança de esquema tem migração explícita, para que
        // nada pendente se perca numa atualização do app.
        .addMigrations(LifeForgeDatabase.MIGRATION_1_2)
        .build()

    @Provides @Singleton
    fun provideUserDao(database: LifeForgeDatabase): UserDao = database.userDao()

    @Provides @Singleton
    fun provideGoalDao(database: LifeForgeDatabase): GoalDao = database.goalDao()

    @Provides @Singleton
    fun provideIncomeDao(database: LifeForgeDatabase): IncomeDao = database.incomeDao()

    @Provides @Singleton
    fun provideExpenseDao(database: LifeForgeDatabase): ExpenseDao = database.expenseDao()

    @Provides @Singleton
    fun provideAssetDao(database: LifeForgeDatabase): AssetDao = database.assetDao()

    @Provides @Singleton
    fun provideSimulationDao(database: LifeForgeDatabase): SimulationDao =
        database.simulationDao()

    @Provides @Singleton
    fun providePendingOperationDao(database: LifeForgeDatabase): PendingOperationDao =
        database.pendingOperationDao()
}
