package com.lifeforge.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeforge.data.sync.SyncableEntity
import java.math.BigDecimal
import java.time.Instant

/**
 * Cache local de metas. Uma meta pertence a um usuário (via [userId]),
 * mas como o app só guarda dados do usuário corrente, não há FK rígida
 * — a relação é mantida pela política de `clearAllTables()` no logout.
 *
 * O índice em `userId` é defensivo: queries futuras com filtro por
 * `userId` (caso um dia o app suporte multi-conta) ficam rápidas.
 *
 * `syncState` segue a convenção das demais entidades offline-first (ver
 * [com.lifeforge.data.sync.SyncState]); id negativo = criada sem conexão.
 */
@Entity(
    tableName = "goals",
    indices = [Index("userId")],
)
data class GoalEntity(
    @PrimaryKey override val id: Long,
    val userId: Long,
    val name: String,
    val category: String,            // GoalCategory.name
    val targetAmount: BigDecimal,    // converter handles
    val targetDate: Instant,         // converter handles
    val priority: Int,
    val createdAt: Instant,
    @ColumnInfo(defaultValue = "SYNCED") override val syncState: String = "SYNCED",
) : SyncableEntity
