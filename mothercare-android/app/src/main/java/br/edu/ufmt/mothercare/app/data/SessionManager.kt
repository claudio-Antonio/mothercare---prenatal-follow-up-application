package br.edu.ufmt.mothercare.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "mothercare_session")

class SessionManager(private val context: Context) {

    private val tokenKey = stringPreferencesKey("token")
    private val usuarioIdKey = stringPreferencesKey("usuario_id")
    private val nomeKey = stringPreferencesKey("nome")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val usuarioIdFlow: Flow<String?> = context.dataStore.data.map { it[usuarioIdKey] }
    val nomeFlow: Flow<String?> = context.dataStore.data.map { it[nomeKey] }

    suspend fun salvarSessao(token: String, usuarioId: String, nome: String) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[usuarioIdKey] = usuarioId
            it[nomeKey] = nome
        }
    }

    suspend fun limparSessao() {
        context.dataStore.edit { it.clear() }
    }

    fun tokenAtualBloqueante(): String? = runBlocking { tokenFlow.first() }
}
