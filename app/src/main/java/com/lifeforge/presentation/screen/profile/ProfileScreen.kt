package com.lifeforge.presentation.screen.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.data.preferences.ThemeMode
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.User
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.LoadingIndicator
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.label

/**
 * Tela de Perfil — versao expandida (Fase 4 final).
 *
 * Secoes (de cima para baixo):
 * 1. Header com avatar circular + nome + email
 * 2. Card "Perfil de risco" com chip atual + botao "Alterar" → dialog
 * 3. Card "Resumo de uso" com contagens (metas, simulacoes via Asset count etc.)
 * 4. Card "Configuracoes" com tema atual + botao para abrir dialog de tema
 * 5. Card "Sobre" com versao, descricao e creditos do TCC
 * 6. Botao "Sair da conta" no rodape (vermelho, destaque negativo)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToParams: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.isRefreshing,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(
                        message = state.errorBanner!!,
                        onDismiss = viewModel::onErrorBannerDismiss,
                    )
                }

                val user = state.user
                if (user == null) {
                    LoadingIndicator()
                } else {
                    HeaderCard(
                        user = user,
                        avatarPath = state.avatarPath,
                        onAvatarPicked = viewModel::onAvatarPicked,
                        onEditName = viewModel::openNameDialog,
                    )
                    RiskProfileCard(
                        user = user,
                        onEditClick = viewModel::openRiskProfileDialog,
                    )
                    SettingsItemCard(
                        icon = Icons.Outlined.Tune,
                        title = "Dados para projeções",
                        subtitle = "Idade, salário, moradia… quanto mais, mais precisas as projeções",
                        onClick = onNavigateToParams,
                    )
                    UsageCard(counts = state.counts)
                    SettingsCard(
                        themeMode = state.themeMode,
                        onThemeClick = viewModel::openThemeDialog,
                    )
                    DynamicColorCard(
                        enabled = state.dynamicColor,
                        onToggle = viewModel::setDynamicColor,
                    )
                    HelpAndFeedbackCard()
                    AboutCard(onClick = viewModel::openAboutDialog)

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sair da conta")
                    }
                }
            }
        }

        // Dialogs
        if (state.showRiskProfileDialog) {
            RiskProfileDialog(
                currentProfile = state.user?.riskProfile ?: RiskProfile.MODERATE,
                isUpdating = state.isUpdatingRiskProfile,
                onConfirm = viewModel::confirmRiskProfileChange,
                onDismiss = viewModel::closeRiskProfileDialog,
            )
        }
        if (state.showThemeDialog) {
            ThemeDialog(
                currentMode = state.themeMode,
                onSelect = viewModel::setThemeMode,
                onDismiss = viewModel::closeThemeDialog,
            )
        }
        if (state.showAboutDialog) {
            AboutDialog(onDismiss = viewModel::closeAboutDialog)
        }
        if (state.showNameDialog) {
            EditNameDialog(
                currentName = state.user?.name.orEmpty(),
                isUpdating = state.isUpdatingName,
                onConfirm = viewModel::confirmNameChange,
                onDismiss = viewModel::closeNameDialog,
            )
        }
    }
}

// ============================================================================
// Header
// ============================================================================

@Composable
private fun HeaderCard(
    user: User,
    avatarPath: String? = null,
    onAvatarPicked: (android.net.Uri) -> Unit = {},
    onEditName: () -> Unit = {},
) {
    // Foto de perfil via Photo Picker do Android (sem permissão de storage).
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onAvatarPicked) }

    // Decodifica o bitmap fora de cada recomposição; o caminho muda a cada
    // troca (timestamp no nome), então remember(avatarPath) recarrega.
    val avatarBitmap = remember(avatarPath) {
        avatarPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(72.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Foto de perfil (toque para trocar)",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = "Adicionar foto de perfil",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.name, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onEditName) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Editar nome",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Cadastrado em ${formatDate(user.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Toque na foto para trocá-la",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    isUpdating: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Editar nome") },
        text = {
            com.lifeforge.presentation.common.LifeForgeTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nome",
                enabled = !isUpdating,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = !isUpdating && name.trim().isNotEmpty(),
            ) {
                Text(if (isUpdating) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("Cancelar")
            }
        },
    )
}

// ============================================================================
// Risk Profile
// ============================================================================

@Composable
private fun RiskProfileCard(user: User, onEditClick: () -> Unit) {
    SettingsItemCard(
        icon = Icons.Outlined.Flag,
        title = "Perfil de risco",
        subtitle = user.riskProfile.label(),
        onClick = onEditClick,
    )
}

@Composable
private fun RiskProfileDialog(
    currentProfile: RiskProfile,
    isUpdating: Boolean,
    onConfirm: (RiskProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(currentProfile) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Alterar perfil de risco") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "O perfil de risco influencia as sugestões de " +
                        "carteira no modo Otimização.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                RiskProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = selected == profile,
                        onClick = { selected = profile },
                        label = { Text(profile.label()) },
                        enabled = !isUpdating,
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                enabled = !isUpdating && selected != currentProfile,
            ) {
                Text(if (isUpdating) "Salvando..." else "Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("Cancelar")
            }
        },
    )
}

// ============================================================================
// Usage Summary
// ============================================================================

@Composable
private fun UsageCard(counts: UsageCounts) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Resumo de uso",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                UsageStat(value = counts.goalsCount.toString(), label = "Metas")
                UsageStat(value = counts.incomesCount.toString(), label = "Receitas")
                UsageStat(value = counts.expensesCount.toString(), label = "Despesas")
                UsageStat(value = counts.assetsCount.toString(), label = "Ativos")
            }
        }
    }
}

@Composable
private fun UsageStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ============================================================================
// Settings (theme)
// ============================================================================

@Composable
private fun SettingsCard(themeMode: ThemeMode, onThemeClick: () -> Unit) {
    SettingsItemCard(
        icon = Icons.Outlined.DarkMode,
        title = "Tema",
        subtitle = themeMode.label(),
        onClick = onThemeClick,
    )
}

@Composable
private fun ThemeDialog(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema do app") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onSelect(mode) },
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(mode.label())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
    )
}

// ============================================================================
// Cores dinâmicas (Material You)
// ============================================================================

/**
 * Personalização opcional: deriva a paleta do papel de parede (Material You).
 * Só aparece no Android 12+; o padrão desligado preserva o tema da marca.
 */
@Composable
private fun DynamicColorCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Cores dinâmicas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Usa as cores do seu papel de parede (Material You)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

// ============================================================================
// Ajuda e feedback
// ============================================================================

/**
 * Padrão "Ajuda e feedback": canal direto com os autores por e-mail, com
 * assunto e versão pré-preenchidos para facilitar o relato.
 */
@Composable
private fun HelpAndFeedbackCard() {
    val context = LocalContext.current
    SettingsItemCard(
        icon = Icons.AutoMirrored.Outlined.HelpOutline,
        title = "Ajuda e feedback",
        subtitle = "Encontrou um problema ou tem uma sugestão? Fale com a gente",
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("gabrielinnocencio22@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "LifeForge — Ajuda e feedback (v1.0.0)")
            }
            runCatching { context.startActivity(intent) }
        },
    )
}

// ============================================================================
// About
// ============================================================================

@Composable
private fun AboutCard(onClick: () -> Unit) {
    SettingsItemCard(
        icon = Icons.Outlined.Info,
        title = "Sobre o LifeForge",
        subtitle = "Versão, créditos e detalhes do TCC",
        onClick = onClick,
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sobre o LifeForge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Versão 1.0.0", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Plataforma de planejamento de vida com simulação de " +
                        "Monte Carlo, otimização financeira e modelo preditivo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "TCC — Trabalho de Conclusão de Curso",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "Autores: Gabriel Innocêncio e Bruno Rodrigues dos Santos\n" +
                        "Orientador: Prof. José Martins Junior",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Stack: Android (Kotlin + Jetpack Compose), " +
                        "Backend (Ktor), Microsserviço ML (Python/FastAPI), " +
                        "Postgres, Docker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}

// ============================================================================
// Shared building block — settings row card
// ============================================================================

/**
 * Linha de configuracao: icone + titulo + subtitulo + chevron a direita.
 * Toda secao "Perfil de risco" / "Tema" / "Sobre" usa este card.
 */
@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ============================================================================
// Local label extension for ThemeMode
// ============================================================================

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Seguir o sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}
