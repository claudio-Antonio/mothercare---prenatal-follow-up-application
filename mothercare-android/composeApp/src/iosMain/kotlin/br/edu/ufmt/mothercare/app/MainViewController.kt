package br.edu.ufmt.mothercare.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.data.criarDataStore
import br.edu.ufmt.mothercare.app.ui.navigation.MotherCareNavGraph
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

fun MainViewController() = ComposeUIViewController {
    val container = AppContainer(criarDataStore())

    MotherCareTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MotherCareNavGraph(container = container)
        }
    }
}
