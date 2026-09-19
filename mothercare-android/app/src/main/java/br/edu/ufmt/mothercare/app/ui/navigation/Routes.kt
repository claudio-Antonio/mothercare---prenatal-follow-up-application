package br.edu.ufmt.mothercare.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val CADASTRO_GESTANTE = "cadastro_gestante"
    const val HOME = "home/{gestanteId}"

    fun home(gestanteId: String) = "home/$gestanteId"
}
