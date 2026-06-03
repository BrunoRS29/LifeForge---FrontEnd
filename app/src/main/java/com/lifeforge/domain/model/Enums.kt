package com.lifeforge.domain.model

/**
 * Enums de domínio. Os valores `.name` são usados no contrato HTTP com o
 * backend Ktor — ordem e nomenclatura precisam ficar idênticas ao
 * `domain/model/Models.kt` do servidor.
 */

/** Perfil de risco do usuário. Define o ponto base do rebalanceamento. */
enum class RiskProfile { CONSERVATIVE, MODERATE, AGGRESSIVE }

/** Categoria semântica de uma meta de vida. */
enum class GoalCategory {
    RETIREMENT,
    REAL_ESTATE,
    FINANCIAL_INDEPENDENCE,
    EDUCATION,
    TRAVEL,
    CUSTOM,
}

/** Origem de uma renda lançada pelo usuário. */
enum class IncomeType { SALARY, BONUS, DIVIDEND, RENT, OTHER }

/** Categoria de despesa para análise mensal e Random Forest preditivo. */
enum class ExpenseCategory {
    HOUSING, FOOD, TRANSPORT, HEALTH, EDUCATION, LEISURE, OTHER,
}

/** Classe de ativo para alocação de carteira e rebalanceamento. */
enum class AssetType {
    FIXED_INCOME, STOCKS, REAL_ESTATE_FUND, CRYPTO, REAL_ESTATE, OTHER,
}

/** Recorrência de um schedule de receita/despesa. Espelha o backend. */
enum class RecurrenceType { ONE_TIME, MONTHLY, INSTALLMENTS }

/** Escopo de uma edição/remoção de schedule: só futuros ou todos. */
enum class ScheduleAffect { FUTURE_ONLY, ALL }
