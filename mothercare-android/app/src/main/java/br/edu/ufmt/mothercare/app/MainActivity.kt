package br.edu.ufmt.mothercare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.edu.ufmt.mothercare.app.ui.navigation.MotherCareNavGraph
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MotherCareApplication).container

        setContent {
            MotherCareTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MotherCareNavGraph(container = container)
                }
            }
        }
    }
}
