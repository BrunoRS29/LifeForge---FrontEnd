package com.lifeforge.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Dropdown reutilizável para seleções enum-like (categoria, tipo, etc.).
 *
 * Usa [ExposedDropdownMenuBox] do Material 3 — âncora no campo de texto,
 * abre/fecha ao tocar; `readOnly = true` impede o teclado de aparecer. O menu
 * segue o Material 3 Expressive: itens com cantos que acompanham a posição na
 * lista e a opção atual marcada (com papel de botão de opção para o TalkBack).
 *
 * Genérico em [T] (normalmente um enum) com [labelOf] para o texto em PT-BR e
 * [iconOf] opcional para um ícone por opção (ex.: categoria de despesa).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    iconOf: ((T) -> ImageVector)? = null,
    supportingText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = { /* readOnly */ },
            readOnly = true,
            label = { Text(label) },
            leadingIcon = iconOf?.let { icon -> { Icon(icon(selected), contentDescription = null) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supportingText?.let { { Text(it) } },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            enabled = enabled,
            singleLine = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MenuDefaults.standaloneGroupShape,
            containerColor = MenuDefaults.groupStandardContainerColor,
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    selected = option == selected,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    text = { Text(labelOf(option)) },
                    shapes = MenuDefaults.itemShape(index = index, count = options.size),
                    leadingIcon = iconOf?.let { icon -> { Icon(icon(option), contentDescription = null) } },
                    selectedLeadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
