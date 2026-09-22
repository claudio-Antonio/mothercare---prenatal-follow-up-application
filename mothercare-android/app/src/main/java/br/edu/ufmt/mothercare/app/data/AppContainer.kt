package br.edu.ufmt.mothercare.app.data

import android.content.Context
import br.edu.ufmt.mothercare.app.data.remote.ApiClient
import br.edu.ufmt.mothercare.app.data.remote.AuthApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.SchedulingApi
import br.edu.ufmt.mothercare.app.data.repository.*

class AppContainer(context: Context) {
    val sessionManager = SessionManager(context)
    private val retrofit = ApiClient.criar(sessionManager)
    private val authApi = retrofit.create(AuthApi::class.java)
    private val clinicalApi = retrofit.create(ClinicalApi::class.java)
    private val schedulingApi = retrofit.create(SchedulingApi::class.java)

    val authRepository = AuthRepository(authApi, sessionManager)
    val gestanteRepository = GestanteRepository(clinicalApi)
    val checklistRepository = ChecklistRepository(clinicalApi)
    val checkInRepository = CheckInRepository(clinicalApi)
    val agendamentoRepository = AgendamentoRepository(schedulingApi)

    val pesoRepository = PesoRepository(clinicalApi)
    val exameRepository = ExameRepository(clinicalApi)
    val prontuarioRepository = ProntuarioRepository(clinicalApi)
}
