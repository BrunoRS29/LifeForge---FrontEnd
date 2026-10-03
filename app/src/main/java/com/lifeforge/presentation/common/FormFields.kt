package com.lifeforge.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

/**
 * Campos de formulário com erro inline. Encapsulam padrões repetitivos
 * do [OutlinedTextField] do Material 3:
 *
 * - `error != null` → habilita o estado `isError` e mostra a mensagem
 *   no `supportingText` (em vermelho).
 * - `singleLine = true` por padrão (forms financeiros não têm campos
 *   multi-linha exceto descrições).
 * - `keyboardOptions` contextual: email com `KeyboardType.Email`,
 *   números com `KeyboardType.Decimal`.
 *
 * Os componentes não rastreiam estado interno — o ViewModel é a fonte
 * única de verdade. Isso simplifica testes e mantém a UI stateless.
 */

@Composable
fun LifeForgeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    suffix: String? = null,
) {
    // Estado interno com TextFieldValue: a região de composição do IME
    // (acentos via dead key/long-press, ex.: "~"+"a" → "ã") precisa viver
    // junto com a seleção, de forma síncrona. Quando o texto era re-emitido
    // pelo ViewModel (String via StateFlow), o round-trip assíncrono
    // cancelava a composição e impedia digitar acentos.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    if (fieldValue.text != value) {
        // Mudança vinda de fora (carregamento, sanitização ou botão de
        // preenchimento): adota o texto externo e move o cursor para o fim.
        fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length))
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            fieldValue = newValue
            if (newValue.text != value) onValueChange(newValue.text)
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text("\u00A0$it") } },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction,
        ),
        enabled = enabled,
        singleLine = singleLine,
        modifier = modifier,
    )
}

/**
 * Campo de senha com toggle de visibilidade. Mantém a mesma API do
 * [LifeForgeTextField] mas adiciona o ícone de olho como `trailingIcon`.
 */
@Composable
fun LifeForgePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else null,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                )
            }
        },
        enabled = enabled,
        singleLine = true,
        modifier = modifier,
    )
}

/** Campo de prazo em meses, com o equivalente em anos logo abaixo ("27 anos e 3 meses"). */
@Composable
fun MonthsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val months = value.toIntOrNull()
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        suffix = { Text("\u00A0meses") },
        supportingText = months?.let { { Text(formatHorizon(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
    )
}

/** Atalho para preencher um capital com a soma dos ativos cadastrados. */
@Composable
fun UseTotalAssetsChip(totalAssets: BigDecimal?, enabled: Boolean, onClick: () -> Unit) {
    if (totalAssets == null || totalAssets.signum() <= 0) return
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text("Usar patrimônio total (${formatBrl(totalAssets)})") },
        leadingIcon = {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary),
    )
}

/**
 * Chave com título e explicação opcional. A linha inteira é o alvo de toque
 * (papel de chave para o TalkBack); o Switch só sinaliza o estado.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    subtitle: String? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}
