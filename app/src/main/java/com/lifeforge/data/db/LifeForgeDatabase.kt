package com.lifeforge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.db.dao.PendingOperationDao
import com.lifeforge.data.db.dao.SimulationDao
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.db.entity.AssetEntity
import com.lifeforge.data.db.entity.ExpenseEntity
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.db.entity.IncomeEntity
import com.lifeforge.data.db.entity.PendingOperationEntity
import com.lifeforge.data.db.entity.SimulationEntity
import com.lifeforge.data.db.entity.SimulationSummaryEntity
import com.lifeforge.data.db.entity.UserEntity

/**
 * Banco local do LifeForge — fonte única de verdade do padrão offline-first.
 *
 * Versões:
 * - **1**: cache das entidades (usuário, metas, receitas, despesas, ativos e
 *   simulações completas).
 * - **2**: escrita offline — coluna `syncState` nas entidades editáveis, fila
 *   de saída `pending_operations`, histórico resumido de simulações
 *   (`simulation_summaries`) e bandas do fan chart no cache de simulações. Migração explícita em [MIGRATION_1_2]: o banco
 *   agora guarda alterações que o usuário fez sem conexão e que não podem ser
 *   perdidas numa recriação.
 *
 * O esquema de cada versão é exportado para `app/schemas/` (revisão de
 * mudanças e base para testes de migração).
 */
@Database(
    entities = [
        UserEntity::class,
        GoalEntity::class,
        IncomeEntity::class,
        ExpenseEntity::class,
        AssetEntity::class,
        SimulationEntity::class,
        PendingOperationEntity::class,
        SimulationSummaryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LifeForgeDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun assetDao(): AssetDao
    abstract fun simulationDao(): SimulationDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        const val DATABASE_NAME = "lifeforge.db"

        /**
         * 1 → 2: suporte a escrita offline. As linhas existentes vieram do
         * servidor, então nascem `SYNCED`; o histórico resumido é semeado a
         * partir das simulações completas já em cache.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("goals", "incomes", "expenses", "assets")) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                db.execSQL("ALTER TABLE `simulations` ADD COLUMN `trajectory` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_operations` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`entityType` TEXT NOT NULL, `entityId` INTEGER NOT NULL, " +
                        "`operation` TEXT NOT NULL, `payload` TEXT, `createdAt` INTEGER NOT NULL, " +
                        "`attempts` INTEGER NOT NULL, `lastError` TEXT)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_operations_entityType_entityId` " +
                        "ON `pending_operations` (`entityType`, `entityId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `simulation_summaries` (" +
                        "`id` INTEGER NOT NULL, `goalId` INTEGER NOT NULL, " +
                        "`successProbability` REAL NOT NULL, `mean` REAL NOT NULL, " +
                        "`median` REAL NOT NULL, `targetAmount` REAL NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulation_summaries_goalId` " +
                        "ON `simulation_summaries` (`goalId`)"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO `simulation_summaries` " +
                        "(`id`, `goalId`, `successProbability`, `mean`, `median`, `targetAmount`, `createdAt`) " +
                        "SELECT `id`, `goalId`, `successProbability`, `mean`, `median`, `targetAmount`, `createdAt` " +
                        "FROM `simulations`"
                )
            }
        }
    }
}
