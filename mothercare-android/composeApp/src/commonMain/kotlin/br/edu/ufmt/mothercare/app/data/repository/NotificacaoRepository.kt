package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.NotificacaoResponse

/** RF08: notificações in-app. Vale para gestante e médico (o backend filtra pelo usuário do token). */
class NotificacaoRepository(private val api: ClinicalApi) {
    suspend fun listar(): Resultado<List<NotificacaoResponse>> = chamarApi { api.notificacoes() }

    suspend fun marcarComoLida(id: String): Resultado<NotificacaoResponse> = chamarApi { api.marcarNotificacaoLida(id) }
}
