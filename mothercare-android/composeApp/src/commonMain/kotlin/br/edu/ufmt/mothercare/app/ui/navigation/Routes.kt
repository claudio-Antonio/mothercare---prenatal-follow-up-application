package br.edu.ufmt.mothercare.app.ui.navigation

import io.ktor.http.encodeURLParameter

object Routes {
    /** Decide a primeira tela a partir da sessão salva (evita pedir login a cada abertura do app). */
    const val INICIO = "inicio"
    const val LOGIN = "login?expirada={expirada}"
    const val CADASTRO = "cadastro"
    const val CADASTRO_GESTANTE = "cadastro_gestante"
    const val HOME = "home/{gestanteId}"

    const val PESO = "peso/{gestanteId}"
    const val EXAME = "exame/{gestanteId}"
    const val PRONTUARIO = "prontuario/{gestanteId}?categoria={categoria}"
    const val PERFIL = "perfil"
    /** RF08: caixa de notificações. gestanteId opcional: só a gestante tem lembrete de consulta. */
    const val NOTIFICACOES = "notificacoes?gestanteId={gestanteId}"

    // Painel do médico — sem gestanteId, é escopado pela identidade do token
    const val MEDICO_DASHBOARD = "medico_dashboard"
    const val MEDICO_GESTANTE = "medico/gestante/{gestanteId}?nome={nome}"

    fun login(sessaoExpirada: Boolean = false) = "login?expirada=$sessaoExpirada"
    fun home(gestanteId: String) = "home/$gestanteId"
    fun peso(gestanteId: String) = "peso/$gestanteId"
    fun exame(gestanteId: String) = "exame/$gestanteId"
    /** [categoria]: vindo do checklist — o prontuário já abre o envio do laudo com o nome do exame. */
    fun prontuario(gestanteId: String, categoria: String? = null) =
        "prontuario/$gestanteId" + (categoria?.let { "?categoria=${it.encodeURLParameter()}" } ?: "")
    fun notificacoes(gestanteId: String?) = "notificacoes" + (gestanteId?.let { "?gestanteId=$it" } ?: "")
    fun medicoGestante(gestanteId: String, nome: String?) =
        "medico/gestante/$gestanteId" + (nome?.let { "?nome=${it.encodeURLParameter()}" } ?: "")
}
