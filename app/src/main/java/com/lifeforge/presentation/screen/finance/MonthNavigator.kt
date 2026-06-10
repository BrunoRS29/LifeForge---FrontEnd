package com.lifeforge.presentation.screen.finance

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatMonthYear
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Cabeçalho de navegação por mês (‹ Junho 2026 ›) com o total do mês.
 * Compartilhado por Receitas e Despesas para o controle mensal (Sprint 6).
 *
 * - Toque no nome do mês abre o seletor de data nativo do Android
 *   (escolher qualquer dia do mês seleciona aquele mês/ano).
 * - Quando o mês exibido não é o atual, aparece o atalho
 *   "Voltar ao mês atual".
 */
@Composable
fun MonthNavigator(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    total: BigDecimal,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val context = LocalContext.current
    val currentMonth = YearMonth.now()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
            Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    // Picker nativo do Android: o dia escolhido é ignorado,
                    // só o mês/ano interessam.
                    DatePickerDialog(
                        context,
                        { _, year, monthZeroBased, _ ->
                            onMonthChange(YearMonth.of(year, monthZeroBased + 1))
                        },
                        month.year,
                        month.monthValue - 1,
                        1,
                    ).show()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                formatMonthYear(month),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                buildString {
                    if (count != null) append("$count ${if (count == 1) "lançamento" else "lançamentos"} · ")
                    append("Total: ${formatBrl(total)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (month != currentMonth) {
                TextButton(onClick = { onMonthChange(currentMonth) }) {
                    Text("Voltar ao mês atual")
                }
            }
        }
        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
    }
}
