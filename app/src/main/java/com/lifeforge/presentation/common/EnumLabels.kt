package com.lifeforge.presentation.common

import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.RiskProfile

/**
 * Labels PT-BR centralizados para os enums do domain. Ficam aqui para
 * evitar duplicação entre telas — Goal, Finance, Profile e Optimization
 * todos precisam dos mesmos rótulos.
 *
 * Se a UX pedir tradução para outros idiomas (improvável no escopo do
 * TCC), basta substituir estas funções pelo `stringResource` do Compose.
 */

fun GoalCategory.label(): String = when (this) {
    GoalCategory.RETIREMENT -> "Aposentadoria"
    GoalCategory.REAL_ESTATE -> "Imóvel"
    GoalCategory.FINANCIAL_INDEPENDENCE -> "Independência financeira"
    GoalCategory.EDUCATION -> "Educação"
    GoalCategory.TRAVEL -> "Viagem"
    GoalCategory.CUSTOM -> "Personalizada"
}

fun IncomeType.label(): String = when (this) {
    IncomeType.SALARY -> "Salário"
    IncomeType.BONUS -> "Bônus"
    IncomeType.DIVIDEND -> "Dividendos"
    IncomeType.RENT -> "Aluguel"
    IncomeType.OTHER -> "Outro"
}

fun ExpenseCategory.label(): String = when (this) {
    ExpenseCategory.HOUSING -> "Moradia"
    ExpenseCategory.FOOD -> "Alimentação"
    ExpenseCategory.TRANSPORT -> "Transporte"
    ExpenseCategory.HEALTH -> "Saúde"
    ExpenseCategory.EDUCATION -> "Educação"
    ExpenseCategory.LEISURE -> "Lazer"
    ExpenseCategory.OTHER -> "Outro"
}

fun AssetType.label(): String = when (this) {
    AssetType.FIXED_INCOME -> "Renda fixa"
    AssetType.STOCKS -> "Ações"
    AssetType.REAL_ESTATE_FUND -> "FII"
    AssetType.CRYPTO -> "Cripto"
    AssetType.REAL_ESTATE -> "Imóvel"
    AssetType.OTHER -> "Outro"
}

fun RiskProfile.label(): String = when (this) {
    RiskProfile.CONSERVATIVE -> "Conservador"
    RiskProfile.MODERATE -> "Moderado"
    RiskProfile.AGGRESSIVE -> "Arrojado"
}
