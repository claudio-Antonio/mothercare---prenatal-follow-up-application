package br.edu.ufmt.mothercare.app

import android.app.Application
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.data.criarDataStore

class MotherCareApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(criarDataStore(this))
    }
}
