package br.edu.ufmt.mothercare.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val CADASTRO_GESTANTE = "cadastro_gestante"
    const val HOME = "home/{gestanteId}"

    const val PESO = "peso/{gestanteId}"
    const val EXAME = "exame/{gestanteId}"
    const val PRONTUARIO = "prontuario/{gestanteId}"

    fun home(gestanteId: String) = "home/$gestanteId"
    fun peso(gestanteId: String) = "peso/$gestanteId"
    fun exame(gestanteId: String) = "exame/$gestanteId"
    fun prontuario(gestanteId: String) = "prontuario/$gestanteId"
}
