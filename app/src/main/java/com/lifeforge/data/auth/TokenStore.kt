package com.lifeforge.data.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore para persistir o token JWT entre sessões do app.
 *
 * Decisões:
 * - DataStore Preferences (não Proto) porque temos um único valor pequeno.
 * - Nome "auth_prefs" referenciado em `data_extraction_rules.xml` para
 *   excluir do backup automático do Android.
 * - O token NÃO é criptografado em repouso — para um TCC isso é
 *   aceitável; para produção real, encapsular em EncryptedSharedPreferences
 *   ou usar o Android Keystore.
 *
 * Marcado como Singleton para que o mesmo Flow seja compartilhado entre
 * Interceptor e ViewModels.
 */
private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val TOKEN: Preferences.Key<String> = stringPreferencesKey("jwt_token")
    }

    /** Flow do token atual; emite null quando não há sessão. */
    val tokenFlow: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[Keys.TOKEN] }

    /** Leitura síncrona do token — usada pelo Interceptor (bloqueante por design). */
    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun setToken(token: String) {
        context.authDataStore.edit { prefs -> prefs[Keys.TOKEN] = token }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs -> prefs.remove(Keys.TOKEN) }
    }
}
