package com.lifeforge.presentation.screen.profile

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.EmploymentType
import com.lifeforge.domain.model.UserProfile
import com.lifeforge.domain.repository.UserProfileRepository
import com.lifeforge.domain.usecase.GetUserProfileUseCase
import com.lifeforge.domain.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Testa o ViewModel da tela de parametros do perfil: o carregamento popula o
 * formulario a partir do perfil salvo, e o salvar mapeia o formulario de volta
 * para o perfil e persiste. Usa um dispatcher de teste como Main (viewModelScope).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileParamsViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeProfileRepo(private val initial: UserProfile) : UserProfileRepository {
        var saved: UserProfile? = null
        override suspend fun getProfile(): DataResult<UserProfile> = DataResult.Success(initial)
        override suspend fun updateProfile(profile: UserProfile): DataResult<UserProfile> {
            saved = profile
            return DataResult.Success(profile)
        }
    }

    private fun viewModel(repo: UserProfileRepository) =
        ProfileParamsViewModel(GetUserProfileUseCase(repo), UpdateUserProfileUseCase(repo))

    @Test
    fun `load popula o formulario a partir do perfil salvo`() = runTest {
        val repo = FakeProfileRepo(
            UserProfile(age = 30, monthlySalary = "5000", employmentType = EmploymentType.CLT)
        )

        val vm = viewModel(repo)
        val form = vm.state.value.form

        assertThat(form.age).isEqualTo("30")
        assertThat(form.monthlySalary).isEqualTo("5000")
        assertThat(form.employmentType).isEqualTo(EmploymentType.CLT)
    }

    @Test
    fun `save mapeia o formulario para perfil e persiste`() = runTest {
        val repo = FakeProfileRepo(UserProfile.EMPTY)
        val vm = viewModel(repo)

        vm.update { it.copy(age = "40", monthlySalary = "18480") }
        vm.save()

        assertThat(repo.saved?.age).isEqualTo(40)
        assertThat(repo.saved?.monthlySalary).isEqualTo("18480")
        assertThat(vm.state.value.message).isEqualTo("Perfil salvo.")
    }
}
