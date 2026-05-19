package com.lifeforge.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modo de tema escolhido pelo usuario. Sobrescreve o
 * `isSystemInDarkTheme()` do Compose quando diferente de [SYSTEM].
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * DataStore separado do token de autenticacao — preferencias do app
 * que sobrevivem ao logout (ex.: tema).
 *
 * O token fica em `auth_prefs` ([com.lifeforge.data.auth.TokenStore]) e e
 * apagado no logout. Este aqui vive em `app_prefs` e e preservado
 * entre sessoes — se o usuario gostar de tema escuro, mantemos
 * mesmo apos sair e entrar de novo.
 */
@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.appPreferencesDataStore

    /** Flow do tema atual; default [ThemeMode.SYSTEM]. */
    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

// Extension declarada em top-level — DataStore exige que a instancia
// seja singleton por arquivo, garantido por este delegate.
private val Context.appPreferencesDataStore: androidx.datastore.core.DataStore<Preferences>
        by preferencesDataStore(name = "app_prefs")
