package com.lifeforge.presentation.screen.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.onFailure
import com.lifeforge.domain.usecase.CreateAssetUseCase
import com.lifeforge.domain.usecase.DeleteAssetUseCase
import com.lifeforge.domain.usecase.ObserveAssetsUseCase
import com.lifeforge.domain.usecase.RefreshAssetsUseCase
import com.lifeforge.domain.usecase.UpdateAssetUseCase
import com.lifeforge.presentation.common.parseCurrencyInput
import com.lifeforge.presentation.common.sanitizeCurrencyInput
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da sub-aba de Ativos. Diferente das outras duas:
 *
 * 1. Tem mais campos (currentValue, expectedReturn, volatility) que
 *    são todos monetários/percentuais — usa o [com.lifeforge.presentation.common.CurrencyField].
 * 2. Suporta **edição** (toque num card abre o form pré-preenchido).
 *    Income/Expense não têm edição porque o backend deles não expõe
 *    PUT — apenas POST e DELETE.
 *
 * O form modal carrega o id do ativo via [AssetFormState.editingId]:
 * `null` → modo criação; não-nulo → modo edição.
 */
@HiltViewModel
class AssetViewModel @Inject constructor(
    observeAssets: ObserveAssetsUseCase,
    private val refreshAssets: RefreshAssetsUseCase,
    private val createAsset: CreateAssetUseCase,
    private val updateAsset: UpdateAssetUseCase,
    private val deleteAsset: DeleteAssetUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    val state: StateFlow<AssetUiState> = combine(
        observeAssets(),
        localState,
    ) { assets, local ->
        AssetUiState(
            assets = assets,
            isRefreshing = local.isRefreshing,
            errorBanner = local.errorBanner,
            form = local.form,
            isSubmitting = local.isSubmitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssetUiState(),
    )

    init { refresh() }

    fun refresh() {
        if (localState.value.isRefreshing) return
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true, errorBanner = null) }
            refreshAssets().onFailure { error ->
                localState.update { it.copy(errorBanner = error.toUserMessage()) }
            }
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    fun openCreateForm() {
        localState.update { it.copy(form = AssetFormState()) }
    }

    fun openEditForm(asset: Asset) {
        localState.update {
            it.copy(
                form = AssetFormState(
                    editingId = asset.id,
                    name = asset.name,
                    assetType = asset.assetType,
                    currentValueInput = asset.currentValue.toPlainString(),
                    expectedReturnInput = asset.expectedReturn.toPlainString(),
                    volatilityInput = asset.volatility.toPlainString(),
                ),
            )
        }
    }

    fun closeForm() {
        localState.update { it.copy(form = null) }
    }

    fun onFormNameChange(name: String) = updateForm { it.copy(name = name, nameError = null) }
    fun onFormTypeChange(type: AssetType) = updateForm { it.copy(assetType = type) }
    fun onFormCurrentValueChange(input: String) = updateForm {
        it.copy(currentValueInput = sanitizeCurrencyInput(input), currentValueError = null)
    }
    fun onFormExpectedReturnChange(input: String) = updateForm {
        it.copy(expectedReturnInput = sanitizeCurrencyInput(input), expectedReturnError = null)
    }
    fun onFormVolatilityChange(input: String) = updateForm {
        it.copy(volatilityInput = sanitizeCurrencyInput(input), volatilityError = null)
    }

    private inline fun updateForm(crossinline mutate: (AssetFormState) -> AssetFormState) {
        localState.update { local ->
            local.form?.let { f -> local.copy(form = mutate(f)) } ?: local
        }
    }

    fun submitForm() {
        val form = localState.value.form ?: return
        if (localState.value.isSubmitting) return

        val currentValue = parseCurrencyInput(form.currentValueInput)
        val expectedReturn = parseCurrencyInput(form.expectedReturnInput)
        val volatility = parseCurrencyInput(form.volatilityInput)

        // Valida os três valores antes de submeter; mostra todos os erros.
        if (currentValue == null || expectedReturn == null || volatility == null) {
            localState.update { local ->
                local.form?.let { f ->
                    local.copy(form = f.copy(
                        currentValueError = if (currentValue == null) "Valor inválido" else null,
                        expectedReturnError = if (expectedReturn == null) "Valor inválido" else null,
                        volatilityError = if (volatility == null) "Valor inválido" else null,
                    ))
                } ?: local
            }
            return
        }

        viewModelScope.launch {
            localState.update { it.copy(isSubmitting = true, errorBanner = null) }
            val result = if (form.editingId == null) {
                createAsset(
                    name = form.name,
                    assetType = form.assetType,
                    currentValue = currentValue,
                    expectedReturn = expectedReturn,
                    volatility = volatility,
                )
            } else {
                updateAsset(
                    id = form.editingId,
                    name = form.name,
                    assetType = form.assetType,
                    currentValue = currentValue,
                    expectedReturn = expectedReturn,
                    volatility = volatility,
                )
            }
            when (result) {
                is DataResult.Success -> localState.update {
                    it.copy(form = null, isSubmitting = false)
                }
                is DataResult.Failure -> {
                    handleFormError(result.error)
                    localState.update { it.copy(isSubmitting = false) }
                }
            }
        }
    }

    private fun handleFormError(error: AppError) {
        when (error) {
            is AppError.Validation -> when (error.field) {
                "name" -> updateForm { it.copy(nameError = error.message) }
                "currentValue" -> updateForm { it.copy(currentValueError = error.message) }
                "volatility" -> updateForm { it.copy(volatilityError = error.message) }
                else -> localState.update {
                    it.copy(errorBanner = error.message ?: "Dados inválidos")
                }
            }
            else -> localState.update { it.copy(errorBanner = error.toUserMessage()) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteAsset(id).onFailure { error ->
                localState.update { it.copy(errorBanner = error.toUserMessage()) }
            }
        }
    }

    fun onErrorBannerDismiss() {
        localState.update { it.copy(errorBanner = null) }
    }

    private data class LocalUiState(
        val isRefreshing: Boolean = false,
        val errorBanner: String? = null,
        val form: AssetFormState? = null,
        val isSubmitting: Boolean = false,
    )
}

data class AssetUiState(
    val assets: List<Asset> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
    val form: AssetFormState? = null,
    val isSubmitting: Boolean = false,
)

data class AssetFormState(
    val editingId: Long? = null,
    val name: String = "",
    val assetType: AssetType = AssetType.FIXED_INCOME,
    val currentValueInput: String = "",
    val expectedReturnInput: String = "",
    val volatilityInput: String = "",
    val nameError: String? = null,
    val currentValueError: String? = null,
    val expectedReturnError: String? = null,
    val volatilityError: String? = null,
) {
    val isEditing: Boolean get() = editingId != null
    val canSubmit: Boolean
        get() = name.isNotBlank() &&
            currentValueInput.isNotBlank() &&
            expectedReturnInput.isNotBlank() &&
            volatilityInput.isNotBlank()
}
