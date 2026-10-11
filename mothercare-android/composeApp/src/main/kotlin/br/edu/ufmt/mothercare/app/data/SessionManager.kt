package br.edu.ufmt.mothercare.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Snapshot da sessão salva — usado pra decidir a tela inicial ao abrir o app. */
data class Sessao(
    val token: String?,
    val usuarioId: String?,
    val papel: String?,
    val gestanteId: String?
)

class SessionManager(private val dataStore: DataStore<Preferences>) {

    private val tokenKey = stringPreferencesKey("token")
    private val usuarioIdKey = stringPreferencesKey("usuario_id")
    private val nomeKey = stringPreferencesKey("nome")
    private val papelKey = stringPreferencesKey("papel")
    private val gestanteIdKey = stringPreferencesKey("gestante_id")

    val tokenFlow: Flow<String?> = dataStore.data.map { it[tokenKey] }
    val usuarioIdFlow: Flow<String?> = dataStore.data.map { it[usuarioIdKey] }
    val nomeFlow: Flow<String?> = dataStore.data.map { it[nomeKey] }

    private val _sessaoExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emite quando o backend responde 401 numa rota autenticada (token expirado/inválido). */
    val sessaoExpirada: SharedFlow<Unit> = _sessaoExpirada.asSharedFlow()

    suspend fun salvarSessao(token: String, usuarioId: String, nome: String, papel: String) {
        dataStore.edit {
            it.clear()
            it[tokenKey] = token
            it[usuarioIdKey] = usuarioId
            it[nomeKey] = nome
            it[papelKey] = papel
        }
    }

    suspend fun salvarGestanteId(gestanteId: String) {
        dataStore.edit { it[gestanteIdKey] = gestanteId }
    }

    suspend fun obterSessao(): Sessao {
        val prefs = dataStore.data.first()
        return Sessao(prefs[tokenKey], prefs[usuarioIdKey], prefs[papelKey], prefs[gestanteIdKey])
    }

    suspend fun limparSessao() {
        dataStore.edit { it.clear() }
    }

    suspend fun obterTokenAtual(): String? = tokenFlow.first()

    /** Chamado pelo ApiClient quando o backend responde 401 numa rota autenticada. */
    suspend fun expirarSessao() {
        limparSessao()
        _sessaoExpirada.tryEmit(Unit)
    }
}
