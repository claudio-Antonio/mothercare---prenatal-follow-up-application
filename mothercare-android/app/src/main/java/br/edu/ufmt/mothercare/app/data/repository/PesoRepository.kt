package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.GraficoPesoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.RegistroPesoRequest

class PesoRepository(private val api: ClinicalApi) {
    suspend fun registrar(gestanteId: String, pesoKg: Double): Resultado<GraficoPesoResponse> =
        chamarApi { api.registrarPeso(RegistroPesoRequest(gestanteId, pesoKg)) }

    suspend fun obterGrafico(gestanteId: String): Resultado<GraficoPesoResponse> =
        chamarApi { api.obterGraficoPeso(gestanteId) }
}
