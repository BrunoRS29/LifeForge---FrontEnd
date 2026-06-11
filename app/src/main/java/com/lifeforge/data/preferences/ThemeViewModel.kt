package com.lifeforge.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.data.preferences.AppPreferencesStore
import com.lifeforge.data.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel raiz que expoe a preferencia de tema para a [com.lifeforge.presentation.MainActivity].
 *
 * Roda no escopo do Activity (e nao do Composable de tela) para evitar
 * que o tema "pisque" durante navegacao — a preferencia e estavel desde
 * que o Activity comecou.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    appPreferences: AppPreferencesStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )

    /** Cores dinâmicas (Material You); default desligado = tema da marca. */
    val dynamicColor: StateFlow<Boolean> = appPreferences.dynamicColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )
}
