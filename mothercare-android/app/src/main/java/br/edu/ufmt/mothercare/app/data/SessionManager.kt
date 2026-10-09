package br.edu.ufmt.mothercare.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "mothercare_session")

/** Snapshot da sessão salva — usado pra decidir a tela inicial ao abrir o app. */
data class Sessao(
    val token: String?,
    val usuarioId: String?,
    val papel: String?,
    val gestanteId: String?
)

class SessionManager(private val context: Context) {

    private val tokenKey = stringPreferencesKey("token")
    private val usuarioIdKey = stringPreferencesKey("usuario_id")
    private val nomeKey = stringPreferencesKey("nome")
    private val papelKey = stringPreferencesKey("papel")
    private val gestanteIdKey = stringPreferencesKey("gestante_id")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val usuarioIdFlow: Flow<String?> = context.dataStore.data.map { it[usuarioIdKey] }
    val nomeFlow: Flow<String?> = context.dataStore.data.map { it[nomeKey] }

    private val _sessaoExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emite quando o backend responde 401 numa rota autenticada (token expirado/inválido). */
    val sessaoExpirada: SharedFlow<Unit> = _sessaoExpirada.asSharedFlow()

    suspend fun salvarSessao(token: String, usuarioId: String, nome: String, papel: String) {
        context.dataStore.edit {
            it.clear()
            it[tokenKey] = token
            it[usuarioIdKey] = usuarioId
            it[nomeKey] = nome
            it[papelKey] = papel
        }
    }

    suspend fun salvarGestanteId(gestanteId: String) {
        context.dataStore.edit { it[gestanteIdKey] = gestanteId }
    }

    suspend fun obterSessao(): Sessao {
        val prefs = context.dataStore.data.first()
        return Sessao(prefs[tokenKey], prefs[usuarioIdKey], prefs[papelKey], prefs[gestanteIdKey])
    }

    suspend fun limparSessao() {
        context.dataStore.edit { it.clear() }
    }

    fun tokenAtualBloqueante(): String? = runBlocking { tokenFlow.first() }

    /** Chamado pelo AuthInterceptor (thread do OkHttp, fora da main thread). */
    fun expirarSessaoBloqueante() {
        runBlocking { limparSessao() }
        _sessaoExpirada.tryEmit(Unit)
    }
}
