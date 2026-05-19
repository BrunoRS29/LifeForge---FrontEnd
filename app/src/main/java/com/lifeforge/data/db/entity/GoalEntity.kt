package com.lifeforge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

/**
 * Cache local de metas. Uma meta pertence a um usuário (via [userId]),
 * mas como o app só guarda dados do usuário corrente, não há FK rígida
 * — a relação é mantida pela política de `clearAllTables()` no logout.
 *
 * O índice em `userId` é defensivo: queries futuras com filtro por
 * `userId` (caso um dia o app suporte multi-conta) ficam rápidas.
 */
@Entity(
    tableName = "goals",
    indices = [Index("userId")],
)
data class GoalEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val category: String,            // GoalCategory.name
    val targetAmount: BigDecimal,    // converter handles
    val targetDate: Instant,         // converter handles
    val priority: Int,
    val createdAt: Instant,
)
