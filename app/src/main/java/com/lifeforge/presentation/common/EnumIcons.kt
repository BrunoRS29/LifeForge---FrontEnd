package com.lifeforge.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Elderly
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.IncomeType

/**
 * Ícones por categoria (estilo contorno em todo o app — diretriz: um estilo de
 * ícone só). Ajudam a escanear listas; o texto ao lado continua sendo a
 * informação, então os ícones são decorativos para o leitor de tela.
 */
fun GoalCategory.icon(): ImageVector = when (this) {
    GoalCategory.RETIREMENT -> Icons.Outlined.Elderly
    GoalCategory.REAL_ESTATE -> Icons.Outlined.Home
    GoalCategory.FINANCIAL_INDEPENDENCE -> Icons.Outlined.Savings
    GoalCategory.EDUCATION -> Icons.Outlined.School
    GoalCategory.TRAVEL -> Icons.Outlined.Flight
    GoalCategory.CUSTOM -> Icons.Outlined.Flag
}

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.HOUSING -> Icons.Outlined.Home
    ExpenseCategory.FOOD -> Icons.Outlined.ShoppingCart
    ExpenseCategory.TRANSPORT -> Icons.Outlined.DirectionsCar
    ExpenseCategory.HEALTH -> Icons.Outlined.LocalHospital
    ExpenseCategory.EDUCATION -> Icons.Outlined.School
    ExpenseCategory.LEISURE -> Icons.Outlined.Celebration
    ExpenseCategory.OTHER -> Icons.Outlined.Category
}

fun IncomeType.icon(): ImageVector = when (this) {
    IncomeType.SALARY -> Icons.Outlined.Work
    IncomeType.BONUS -> Icons.Outlined.CardGiftcard
    IncomeType.DIVIDEND -> Icons.Outlined.Paid
    IncomeType.RENT -> Icons.Outlined.Key
    IncomeType.OTHER -> Icons.Outlined.Payments
}

fun AssetType.icon(): ImageVector = when (this) {
    AssetType.FIXED_INCOME -> Icons.Outlined.Savings
    AssetType.STOCKS -> Icons.AutoMirrored.Outlined.ShowChart
    AssetType.REAL_ESTATE_FUND -> Icons.Outlined.Apartment
    AssetType.CRYPTO -> Icons.Outlined.CurrencyBitcoin
    AssetType.REAL_ESTATE -> Icons.Outlined.Home
    AssetType.OTHER -> Icons.Outlined.Category
}
