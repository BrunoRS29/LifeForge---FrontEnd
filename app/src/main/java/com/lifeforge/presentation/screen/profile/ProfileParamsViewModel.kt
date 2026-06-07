package com.lifeforge.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.HousingStatus
import com.lifeforge.domain.model.MaritalStatus
import com.lifeforge.domain.model.RiskLevel
import com.lifeforge.domain.model.TaxRegime
import com.lifeforge.domain.model.UserProfile
import com.lifeforge.domain.usecase.GetUserProfileUseCase
import com.lifeforge.domain.usecase.UpdateUserProfileUseCase
import com.lifeforge.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela de parâmetros do perfil (dados para projeções).
 *
 * Carrega o perfil ao abrir, mantém um [ProfileFormState] editável e salva via
 * PUT. A API da tela é minimalista: um único [update] que recebe uma
 * transformação do form (`update { it.copy(...) }`), evitando dezenas de
 * handlers para ~17 campos.
 */
@HiltViewModel
class ProfileParamsViewModel @Inject constructor(
    private val getProfile: GetUserProfileUseCase,
    private val updateProfile: UpdateUserProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileParamsUiState())
    val state: StateFlow<ProfileParamsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getProfile()) {
                is DataResult.Success -> _state.update {
                    it.copy(isLoading = false, form = result.data.toForm())
                }
                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun update(transform: (ProfileFormState) -> ProfileFormState) {
        _state.update { it.copy(form = transform(it.form)) }
    }

    fun save() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, message = null) }
            when (val result = updateProfile(_state.value.form.toProfile())) {
                is DataResult.Success -> _state.update {
                    it.copy(isSaving = false, form = result.data.toForm(), message = "Perfil salvo.")
                }
                is DataResult.Failure -> _state.update {
                    it.copy(isSaving = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(message = null, error = null) }
}

data class ProfileParamsUiState(
    val form: ProfileFormState = ProfileFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

/** Estado editável do formulário — textos como String, seletores como enum?/Boolean. */
data class ProfileFormState(
    val age: String = "",
    val monthlySalary: String = "",
    val employmentType: EmploymentType? = null,
    val retirementAge: String = "",
    val monthlyContribution: String = "",
    val maritalStatus: MaritalStatus? = null,
    val dependents: String = "",
    val childrenAges: String = "",
    val state: String = "",
    val lifeExpectancy: String = "",
    val expectedSalaryGrowth: String = "",
    val unemploymentRisk: RiskLevel? = null,
    val housingStatus: HousingStatus? = null,
    val housingMonthlyCost: String = "",
    val propertyValue: String = "",
    val vehiclesValue: String = "",
    val taxRegime: TaxRegime? = null,
    val emergencyReserve: String = "",
    val totalDebt: String = "",
    val plansChildren: Boolean = false,
    val plansProperty: Boolean = false,
)

private fun UserProfile.toForm() = ProfileFormState(
    age = age?.toString() ?: "",
    monthlySalary = monthlySalary ?: "",
    employmentType = employmentType,
    retirementAge = retirementAge?.toString() ?: "",
    monthlyContribution = monthlyContribution ?: "",
    maritalStatus = maritalStatus,
    dependents = dependents?.toString() ?: "",
    childrenAges = childrenAges ?: "",
    state = state ?: "",
    lifeExpectancy = lifeExpectancy?.toString() ?: "",
    expectedSalaryGrowth = expectedSalaryGrowth ?: "",
    unemploymentRisk = unemploymentRisk,
    housingStatus = housingStatus,
    housingMonthlyCost = housingMonthlyCost ?: "",
    propertyValue = propertyValue ?: "",
    vehiclesValue = vehiclesValue ?: "",
    taxRegime = taxRegime,
    emergencyReserve = emergencyReserve ?: "",
    totalDebt = totalDebt ?: "",
    plansChildren = plansChildren ?: false,
    plansProperty = plansProperty ?: false,
)

private fun ProfileFormState.toProfile() = UserProfile(
    age = age.toIntOrNull(),
    monthlySalary = monthlySalary.ifBlank { null },
    employmentType = employmentType,
    retirementAge = retirementAge.toIntOrNull(),
    monthlyContribution = monthlyContribution.ifBlank { null },
    maritalStatus = maritalStatus,
    dependents = dependents.toIntOrNull(),
    childrenAges = childrenAges.ifBlank { null },
    state = state.ifBlank { null }?.uppercase(),
    lifeExpectancy = lifeExpectancy.toIntOrNull(),
    expectedSalaryGrowth = expectedSalaryGrowth.ifBlank { null },
    unemploymentRisk = unemploymentRisk,
    housingStatus = housingStatus,
    housingMonthlyCost = housingMonthlyCost.ifBlank { null },
    propertyValue = propertyValue.ifBlank { null },
    vehiclesValue = vehiclesValue.ifBlank { null },
    taxRegime = taxRegime,
    emergencyReserve = emergencyReserve.ifBlank { null },
    totalDebt = totalDebt.ifBlank { null },
    plansChildren = plansChildren,
    plansProperty = plansProperty,
)
