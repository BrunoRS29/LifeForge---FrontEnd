package com.lifeforge.presentation.screen.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.data.preferences.AppPreferencesStore
import com.lifeforge.data.preferences.ThemeMode
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.RiskProfile
import com.lifeforge.domain.model.User
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.ObserveAssetsUseCase
import com.lifeforge.domain.usecase.ObserveCurrentUserUseCase
import com.lifeforge.domain.usecase.ObserveExpensesUseCase
import com.lifeforge.domain.usecase.ObserveGoalsUseCase
import com.lifeforge.domain.usecase.ObserveIncomesUseCase
import com.lifeforge.domain.usecase.RefreshCurrentUserUseCase
import com.lifeforge.domain.usecase.UpdateRiskProfileUseCase
import com.lifeforge.domain.usecase.UpdateUserNameUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel da tela de Perfil — versao expandida da Fase 4 final.
 *
 * Combina 7 fontes:
 * - User atual (Flow do Room)
 * - 4 Flows de contagens (goals, incomes, expenses, assets)
 * - ThemeMode atual (DataStore via [AppPreferencesStore])
 * - Estado local de UI (refreshing, dialogs, banner)
 *
 * Os 4 Flows de contagem alimentam o card "Resumo de uso". Como
 * `combine` aceita ate 5 sources de forma type-safe, agrupei as
 * contagens em [UsageCounts] num combine separado para nao estourar
 * o limite.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
    private val updateRiskProfile: UpdateRiskProfileUseCase,
    private val updateUserName: UpdateUserNameUseCase,
    private val appPreferences: AppPreferencesStore,
    @ApplicationContext private val appContext: Context,
    observeGoals: ObserveGoalsUseCase,
    observeIncomes: ObserveIncomesUseCase,
    observeExpenses: ObserveExpensesUseCase,
    observeAssets: ObserveAssetsUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    /** Contagens agregadas — Flow proprio para nao estourar o limite do `combine`. */
    private val countsFlow = combine(
        observeGoals(),
        observeIncomes(),
        observeExpenses(),
        observeAssets(),
    ) { goals, incomes, expenses, assets ->
        UsageCounts(
            goalsCount = goals.size,
            incomesCount = incomes.size,
            expensesCount = expenses.size,
            assetsCount = assets.size,
        )
    }

    val state: StateFlow<ProfileUiState> = combine(
        observeCurrentUser(),
        countsFlow,
        appPreferences.themeModeFlow,
        appPreferences.avatarPathFlow,
        localState,
    ) { user, counts, themeMode, avatarPath, local ->
        ProfileUiState(
            user = user,
            counts = counts,
            themeMode = themeMode,
            avatarPath = avatarPath,
            isRefreshing = local.isRefreshing,
            isUpdatingRiskProfile = local.isUpdatingRiskProfile,
            isUpdatingName = local.isUpdatingName,
            errorBanner = local.errorBanner,
            showRiskProfileDialog = local.showRiskProfileDialog,
            showThemeDialog = local.showThemeDialog,
            showAboutDialog = local.showAboutDialog,
            showNameDialog = local.showNameDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    init {
        // Refresh em background ao abrir — nao bloqueia a UI.
        refresh()
    }

    fun refresh() {
        if (localState.value.isRefreshing) return
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true, errorBanner = null) }
            refreshCurrentUser().onFailure { error ->
                localState.update { it.copy(errorBanner = error.toUserMessage()) }
            }
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    // ------------------------------------------------------------------------
    // Perfil de risco
    // ------------------------------------------------------------------------

    fun openRiskProfileDialog() {
        localState.update { it.copy(showRiskProfileDialog = true) }
    }

    fun closeRiskProfileDialog() {
        localState.update { it.copy(showRiskProfileDialog = false) }
    }

    fun confirmRiskProfileChange(newProfile: RiskProfile) {
        // No-op se o perfil ja for o atual.
        if (state.value.user?.riskProfile == newProfile) {
            closeRiskProfileDialog()
            return
        }
        viewModelScope.launch {
            localState.update {
                it.copy(isUpdatingRiskProfile = true, errorBanner = null)
            }
            when (val result = updateRiskProfile(newProfile)) {
                is DataResult.Success -> localState.update {
                    it.copy(
                        isUpdatingRiskProfile = false,
                        showRiskProfileDialog = false,
                    )
                }
                is DataResult.Failure -> localState.update {
                    it.copy(
                        isUpdatingRiskProfile = false,
                        errorBanner = result.error.toUserMessage(),
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Nome e foto de perfil
    // ------------------------------------------------------------------------

    fun openNameDialog() {
        localState.update { it.copy(showNameDialog = true) }
    }

    fun closeNameDialog() {
        localState.update { it.copy(showNameDialog = false) }
    }

    fun confirmNameChange(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == state.value.user?.name) {
            closeNameDialog()
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(isUpdatingName = true, errorBanner = null) }
            when (val result = updateUserName(trimmed)) {
                is DataResult.Success -> localState.update {
                    it.copy(isUpdatingName = false, showNameDialog = false)
                }
                is DataResult.Failure -> localState.update {
                    it.copy(
                        isUpdatingName = false,
                        errorBanner = result.error.toUserMessage(),
                    )
                }
            }
        }
    }

    /**
     * Copia a imagem escolhida no picker para o armazenamento interno do app
     * (a permissão do URI do picker é temporária) e grava o caminho no
     * DataStore. Nome com timestamp para o Compose recarregar o bitmap.
     */
    fun onAvatarPicked(uri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(appContext.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@runCatching null
                    // Remove avatares antigos para não acumular arquivos.
                    appContext.filesDir.listFiles { f -> f.name.startsWith("avatar_") && f != file }
                        ?.forEach { it.delete() }
                    file.absolutePath
                }.getOrNull()
            }
            if (saved != null) {
                appPreferences.setAvatarPath(saved)
            } else {
                localState.update { it.copy(errorBanner = "Não foi possível carregar a imagem") }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Tema
    // ------------------------------------------------------------------------

    fun openThemeDialog() {
        localState.update { it.copy(showThemeDialog = true) }
    }

    fun closeThemeDialog() {
        localState.update { it.copy(showThemeDialog = false) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
            localState.update { it.copy(showThemeDialog = false) }
        }
    }

    // ------------------------------------------------------------------------
    // Sobre
    // ------------------------------------------------------------------------

    fun openAboutDialog() {
        localState.update { it.copy(showAboutDialog = true) }
    }

    fun closeAboutDialog() {
        localState.update { it.copy(showAboutDialog = false) }
    }

    private data class LocalUiState(
        val isRefreshing: Boolean = false,
        val isUpdatingRiskProfile: Boolean = false,
        val isUpdatingName: Boolean = false,
        val errorBanner: String? = null,
        val showRiskProfileDialog: Boolean = false,
        val showThemeDialog: Boolean = false,
        val showAboutDialog: Boolean = false,
        val showNameDialog: Boolean = false,
    )
}

data class ProfileUiState(
    val user: User? = null,
    val counts: UsageCounts = UsageCounts(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Caminho local da foto de perfil; null = avatar padrão. */
    val avatarPath: String? = null,
    val isRefreshing: Boolean = false,
    val isUpdatingRiskProfile: Boolean = false,
    val isUpdatingName: Boolean = false,
    val errorBanner: String? = null,
    val showRiskProfileDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val showNameDialog: Boolean = false,
)

data class UsageCounts(
    val goalsCount: Int = 0,
    val incomesCount: Int = 0,
    val expensesCount: Int = 0,
    val assetsCount: Int = 0,
)
