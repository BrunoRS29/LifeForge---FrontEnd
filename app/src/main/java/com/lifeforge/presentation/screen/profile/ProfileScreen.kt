package com.lifeforge.presentation.screen.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeforge.data.preferences.ThemeMode
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.User
import com.lifeforge.domain.repository.SyncStatus
import com.lifeforge.presentation.common.ConnectedChoice
import com.lifeforge.presentation.common.ErrorBanner
import com.lifeforge.presentation.common.GroupItem
import com.lifeforge.presentation.common.LifeForgeTextField
import com.lifeforge.presentation.common.ListGroup
import com.lifeforge.presentation.common.RefreshableBox
import com.lifeforge.presentation.common.ScreenLoading
import com.lifeforge.presentation.common.ScreenPadding
import com.lifeforge.presentation.common.TabTopAppBar
import com.lifeforge.presentation.common.formatDate
import com.lifeforge.presentation.common.formatDateTime
import com.lifeforge.presentation.common.label
import com.lifeforge.presentation.common.readableWidth
import com.lifeforge.presentation.common.syncStatusMessage

/**
 * Perfil, no padrão das configurações do Android: cabeçalho com a foto (numa
 * forma do Material 3 Expressive), o nome e o resumo de uso; depois os grupos
 * Planejamento, Aparência (tema em botões conectados e cores dinâmicas), Dados e
 * sincronização e Ajuda; por fim, "Sair da conta". Puxar para baixo atualiza.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToParams: () -> Unit = {},
    onNavigateToPredictions: () -> Unit = {},
    onNavigateToUsability: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopAppBar(title = "Perfil", scrollBehavior = scrollBehavior) },
    ) { padding ->
        RefreshableBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth()
                    .padding(horizontal = ScreenPadding)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (state.errorBanner != null) {
                    ErrorBanner(message = state.errorBanner!!, onDismiss = viewModel::onErrorBannerDismiss)
                }

                val user = state.user
                if (user == null) {
                    ScreenLoading(modifier = Modifier.heightIn(min = 240.dp))
                    return@Column
                }

                ProfileHeader(
                    user = user,
                    avatarPath = state.avatarPath,
                    onAvatarPicked = viewModel::onAvatarPicked,
                    onEditName = viewModel::openNameDialog,
                )
                UsageRow(counts = state.counts)

                ListGroup(
                    title = "Planejamento",
                    items = listOf(
                        GroupItem(
                            headline = "Perfil de risco",
                            supporting = user.riskProfile.label(),
                            icon = Icons.Outlined.Shield,
                            onClick = viewModel::openRiskProfileDialog,
                        ),
                        GroupItem(
                            headline = "Dados para projeções",
                            supporting = "Idade, salário, moradia… quanto mais, mais precisas as projeções",
                            icon = Icons.Outlined.Tune,
                            onClick = onNavigateToParams,
                        ),
                        GroupItem(
                            headline = "Predições de IA",
                            supporting = "Renda, despesas e patrimônio projetados a partir do seu histórico",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = onNavigateToPredictions,
                        ),
                    ),
                )

                AppearanceGroup(
                    themeMode = state.themeMode,
                    onThemeChange = viewModel::setThemeMode,
                    dynamicColor = state.dynamicColor,
                    onDynamicColorChange = viewModel::setDynamicColor,
                )

                ListGroup(
                    title = "Dados e sincronização",
                    items = listOf(syncItem(state.syncStatus, state.isSyncing, viewModel::syncNow)),
                )

                ListGroup(
                    title = "Ajuda e informações",
                    items = listOf(
                        GroupItem(
                            headline = "Avaliação de usabilidade (SUS)",
                            supporting = "Tarefas cronometradas e questionário dos testes com usuários",
                            icon = Icons.AutoMirrored.Outlined.FactCheck,
                            onClick = onNavigateToUsability,
                        ),
                        GroupItem(
                            headline = "Ajuda e feedback",
                            supporting = "Encontrou um problema ou tem uma sugestão? Fale com a gente",
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            onClick = { sendFeedbackEmail(context) },
                        ),
                        GroupItem(
                            headline = "Sobre o LifeForge",
                            supporting = "Versão, créditos e detalhes do TCC",
                            icon = Icons.Outlined.Info,
                            onClick = viewModel::openAboutDialog,
                        ),
                    ),
                )

                FilledTonalButton(
                    onClick = {
                        // Alterações ainda não enviadas se perderiam ao sair: confirma antes.
                        if (state.syncStatus.pendingOperations > 0) viewModel.openLogoutConfirm() else onLogout()
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Text("Sair da conta", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // Diálogos
        if (state.showRiskProfileDialog) {
            RiskProfileDialog(
                currentProfile = state.user?.riskProfile ?: RiskProfile.MODERATE,
                isUpdating = state.isUpdatingRiskProfile,
                onConfirm = viewModel::confirmRiskProfileChange,
                onDismiss = viewModel::closeRiskProfileDialog,
            )
        }
        if (state.showAboutDialog) {
            AboutDialog(onDismiss = viewModel::closeAboutDialog)
        }
        if (state.showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = viewModel::closeLogoutConfirm,
                icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                title = { Text("Sair com alterações não enviadas?") },
                text = {
                    Text(
                        "Há ${state.syncStatus.pendingOperations} alteração(ões) salvas só neste aparelho. " +
                            "Ao sair, elas serão descartadas. Conecte-se à internet e sincronize antes, " +
                            "se quiser mantê-las.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.closeLogoutConfirm()
                        onLogout()
                    }) { Text("Sair mesmo assim", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = viewModel::closeLogoutConfirm) { Text("Cancelar") } },
            )
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
// Cabeçalho
// ============================================================================

/**
 * Foto (Photo Picker do Android, sem permissão de armazenamento) recortada numa
 * forma do Material, com o selo de câmera indicando que dá para trocá-la; nome
 * com ação de editar, e-mail e data de cadastro.
 */
@Composable
private fun ProfileHeader(
    user: User,
    avatarPath: String?,
    onAvatarPicked: (Uri) -> Unit,
    onEditName: () -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onAvatarPicked)
    }
    // Decodifica o bitmap fora de cada recomposição; o caminho muda a cada
    // troca (timestamp no nome), então remember(avatarPath) recarrega.
    val avatarBitmap = remember(avatarPath) {
        avatarPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    val avatarShape = MaterialShapes.Cookie9Sided.toShape()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Surface(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                shape = avatarShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(112.dp)
                    .semantics {
                        contentDescription = if (avatarBitmap != null) "Foto de perfil, toque para trocar" else "Adicionar foto de perfil"
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(avatarShape),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                user.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            IconButton(onClick = onEditName) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Editar nome",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Cadastrado em ${formatDate(user.createdAt)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Resumo de uso: quantos registros de cada tipo o usuário tem. */
@Composable
private fun UsageRow(counts: UsageCounts) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UsageStat(counts.goalsCount, "Metas", Modifier.weight(1f))
        UsageStat(counts.incomesCount, "Receitas", Modifier.weight(1f))
        UsageStat(counts.expensesCount, "Despesas", Modifier.weight(1f))
        UsageStat(counts.assetsCount, "Ativos", Modifier.weight(1f))
    }
}

@Composable
private fun UsageStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$value $label" },
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================================
// Aparência
// ============================================================================

/**
 * Tema (seguir o sistema, claro ou escuro) em botões conectados, aplicado na
 * hora, e as cores dinâmicas do papel de parede (Android 12+), num grupo
 * segmentado.
 */
@Composable
private fun AppearanceGroup(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val hasDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val count = if (hasDynamicColor) 2 else 1
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Aparência",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ListItemDefaults.segmentedShapes(index = 0, count = count).shape,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tema", style = MaterialTheme.typography.bodyLarge)
                    }
                    ConnectedChoice(
                        options = ThemeMode.entries,
                        selected = themeMode,
                        onSelect = onThemeChange,
                        label = { it.label() },
                        icon = { it.icon() },
                        // O bloco já tem a cor de contêiner padrão dos botões.
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    )
                }
            }
            if (hasDynamicColor) {
                SegmentedListItem(
                    onClick = { onDynamicColorChange(!dynamicColor) },
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = count),
                    colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    leadingContent = {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    supportingContent = {
                        Text("Usa as cores do seu papel de parede (Material You)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingContent = { Switch(checked = dynamicColor, onCheckedChange = null) },
                ) {
                    Text("Cores dinâmicas")
                }
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
}

// ============================================================================
// Sincronização (offline-first)
// ============================================================================

/**
 * Estado da sincronização entre o aparelho e o servidor: o app funciona sem
 * conexão e envia as alterações pendentes quando a rede volta.
 */
private fun syncItem(status: SyncStatus, isSyncing: Boolean, onSyncNow: () -> Unit) = GroupItem(
    headline = "Sincronização",
    supporting = syncStatusMessage(status) + "\n" +
        (status.lastSyncAt?.let { "Última: ${formatDateTime(it)}" } ?: "Ainda não sincronizado neste aparelho"),
    icon = if (status.isOnline) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
    trailing = {
        if (isSyncing) {
            LoadingIndicator(modifier = Modifier.size(36.dp))
        } else {
            TextButton(onClick = onSyncNow, enabled = status.isOnline) { Text("Sincronizar") }
        }
    },
)

// ============================================================================
// Ajuda e feedback
// ============================================================================

/**
 * Padrão "Ajuda e feedback": canal direto com os autores por e-mail, com
 * assunto e versão pré-preenchidos para facilitar o relato.
 */
private fun sendFeedbackEmail(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("gabrielinnocencio22@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "LifeForge — Ajuda e feedback (v1.0.0)")
    }
    runCatching { context.startActivity(intent) }
}

// ============================================================================
// Diálogos
// ============================================================================

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
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        title = { Text("Editar nome") },
        text = {
            LifeForgeTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nome",
                enabled = !isUpdating,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = !isUpdating && name.trim().isNotEmpty()) {
                Text(if (isUpdating) "Salvando…" else "Salvar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isUpdating) { Text("Cancelar") } },
    )
}

/** Perfis de risco como opções com explicação; a escolha atual vem marcada. */
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
        icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
        title = { Text("Perfil de risco") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Define a carteira sugerida na otimização e as premissas da simulação com IA.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    RiskProfile.entries.forEachIndexed { index, profile ->
                        SegmentedListItem(
                            selected = selected == profile,
                            onClick = { selected = profile },
                            enabled = !isUpdating,
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = RiskProfile.entries.size),
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                            supportingContent = { Text(profile.description()) },
                            trailingContent = if (selected == profile) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        ) {
                            Text(profile.label())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }, enabled = !isUpdating && selected != currentProfile) {
                Text(if (isUpdating) "Salvando…" else "Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isUpdating) { Text("Cancelar") } },
    )
}

private fun RiskProfile.description(): String = when (this) {
    RiskProfile.CONSERVATIVE -> "Prioriza a segurança: mais renda fixa, menos oscilação."
    RiskProfile.MODERATE -> "Equilibra risco e retorno."
    RiskProfile.AGGRESSIVE -> "Aceita oscilações maiores em busca de mais retorno."
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
        title = { Text("Sobre o LifeForge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Versão 1.0.0", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Plataforma de planejamento de vida com simulação de Monte Carlo, otimização " +
                        "financeira e modelos preditivos.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("TCC — Trabalho de Conclusão de Curso", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Autores: Gabriel Innocêncio e Bruno Rodrigues dos Santos\nOrientador: Prof. José Martins Junior",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Stack: Android (Kotlin + Jetpack Compose), Backend (Ktor), Microsserviço de IA " +
                        "(Python/FastAPI), PostgreSQL, Docker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}
