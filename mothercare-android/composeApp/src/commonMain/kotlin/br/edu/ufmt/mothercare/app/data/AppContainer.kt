package br.edu.ufmt.mothercare.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import br.edu.ufmt.mothercare.app.data.remote.ApiClient
import br.edu.ufmt.mothercare.app.data.remote.AuthApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.SchedulingApi
import br.edu.ufmt.mothercare.app.data.repository.*

/** Recebe só dependências agnósticas de plataforma; o DataStore é criado em androidMain/iosMain. */
class AppContainer(dataStore: DataStore<Preferences>) {
    val sessionManager = SessionManager(dataStore)
    private val httpClient = ApiClient.criar(sessionManager)
    private val authApi = AuthApi(httpClient)
    private val clinicalApi = ClinicalApi(httpClient)
    private val schedulingApi = SchedulingApi(httpClient)

    val authRepository = AuthRepository(authApi, sessionManager)
    val gestanteRepository = GestanteRepository(clinicalApi, sessionManager)
    val checklistRepository = ChecklistRepository(clinicalApi)
    val checkInRepository = CheckInRepository(clinicalApi)
    val agendamentoRepository = AgendamentoRepository(schedulingApi)
    val pesoRepository = PesoRepository(clinicalApi)
    val exameRepository = ExameRepository(clinicalApi)
    val prontuarioRepository = ProntuarioRepository(clinicalApi)

    // Painel do médico
    val medicoRepository = MedicoRepository(clinicalApi)

    // RF08: notificações (gestante e médico)
    val notificacaoRepository = NotificacaoRepository(clinicalApi)
}
