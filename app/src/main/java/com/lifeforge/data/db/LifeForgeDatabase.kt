package com.lifeforge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifeforge.data.db.dao.AssetDao
import com.lifeforge.data.db.dao.ExpenseDao
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.dao.IncomeDao
import com.lifeforge.data.db.dao.SimulationDao
import com.lifeforge.data.db.dao.UserDao
import com.lifeforge.data.db.entity.AssetEntity
import com.lifeforge.data.db.entity.ExpenseEntity
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.db.entity.IncomeEntity
import com.lifeforge.data.db.entity.SimulationEntity
import com.lifeforge.data.db.entity.UserEntity

/**
 * Banco local do LifeForge — cache para o padrão offline-first.
 *
 * Versão atual: 1. Migrações entram a partir da Sprint 5; durante
 * Sprint 4 usamos `fallbackToDestructiveMigration` (configurado em
 * [com.lifeforge.di.DatabaseModule]) — qualquer mudança em schema
 * apaga e recria o banco.
 *
 * Schema export está desligado para o TCC. Em produção o ideal seria
 * versionar os JSONs gerados em `app/schemas/` para revisão de PR.
 */
@Database(
    entities = [
        UserEntity::class,
        GoalEntity::class,
        IncomeEntity::class,
        ExpenseEntity::class,
        AssetEntity::class,
        SimulationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LifeForgeDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun assetDao(): AssetDao
    abstract fun simulationDao(): SimulationDao

    companion object {
        const val DATABASE_NAME = "lifeforge.db"
    }
}
