package com.lifeforge.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions

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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
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
