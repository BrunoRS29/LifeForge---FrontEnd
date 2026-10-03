package com.lifeforge.presentation.screen.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeforge.presentation.common.AutoSizeText
import com.lifeforge.presentation.common.formatBrl
import com.lifeforge.presentation.common.formatMonthShort
import com.lifeforge.presentation.common.formatMonthYear
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth

/**
 * Cabeçalho do mês das listas de Receitas e Despesas: setas para o mês anterior
 * e o próximo, o total do mês em destaque e a quantidade de lançamentos.
 *
 * - Tocar no nome do mês abre o seletor de mês e ano ([MonthPickerSheet]).
 * - Fora do mês atual, aparece o atalho "Voltar ao mês atual".
 */
@Composable
fun MonthNavigator(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    total: BigDecimal,
    modifier: Modifier = Modifier,
    count: Int? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    val currentMonth = YearMonth.now()
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mês anterior")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        onClick = { pickerOpen = true },
                        shape = MaterialTheme.shapes.small,
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatMonthYear(month),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Escolher mês e ano")
                        }
                    }
                    AutoSizeText(
                        text = formatBrl(total),
                        style = MaterialTheme.typography.headlineSmallEmphasized.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    if (count != null) {
                        Text(
                            if (count == 1) "1 lançamento" else "$count lançamentos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FilledTonalIconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Próximo mês")
                }
            }
            if (month != currentMonth) {
                AssistChip(
                    onClick = { onMonthChange(currentMonth) },
                    label = { Text("Voltar ao mês atual") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    if (pickerOpen) {
        MonthPickerSheet(
            selected = month,
            onSelect = {
                pickerOpen = false
                onMonthChange(it)
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

/**
 * Seletor de mês e ano em folha inferior: o ano com setas e os 12 meses em
 * botões (o escolhido vira pílula; o mês atual fica destacado).
 */
@Composable
private fun MonthPickerSheet(
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var year by remember { mutableIntStateOf(selected.year) }
    val current = YearMonth.now()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Ano anterior")
                }
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                IconButton(onClick = { year++ }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Próximo ano")
                }
            }
            Month.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { month ->
                        val value = YearMonth.of(year, month)
                        val isCurrent = value == current
                        ToggleButton(
                            checked = value == selected,
                            onCheckedChange = { onSelect(value) },
                            colors = if (isCurrent && value != selected) {
                                ToggleButtonDefaults.tonalToggleButtonColors()
                            } else {
                                ToggleButtonDefaults.toggleButtonColors()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                        ) {
                            Text(formatMonthShort(month))
                        }
                    }
                }
            }
        }
    }
}
