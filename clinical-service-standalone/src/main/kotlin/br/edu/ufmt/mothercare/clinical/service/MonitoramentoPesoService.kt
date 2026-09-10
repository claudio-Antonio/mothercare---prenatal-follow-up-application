package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.GraficoPesoResponse
import br.edu.ufmt.mothercare.clinical.dto.PontoGraficoPeso
import br.edu.ufmt.mothercare.clinical.dto.RegistroPesoRequest
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.RegistroPeso
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import br.edu.ufmt.mothercare.clinical.repository.RegistroPesoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit

/**
 * RN05 / UC09 - Monitorar Ganho de Peso.
 * Emite alerta nutricional se o ganho de peso ultrapassar 1,5 kg por mês
 * durante o 2º trimestre (semana 14 à 27), comparando o último registro
 * com o imediatamente anterior e normalizando a taxa para uma janela de
 * 30 dias (ganho/dias * 30), já que as pesagens não ocorrem em intervalos
 * fixos de exatamente um mês.
 */
@Service
class MonitoramentoPesoService(
    private val repository: RegistroPesoRepository,
    private val calculadoraGestacional: CalculadoraGestacionalService
) {
    companion object {
        const val LIMIAR_GANHO_MENSAL_KG = 1.5
    }

    @Transactional
    fun registrar(gestante: Gestante, request: RegistroPesoRequest): GraficoPesoResponse {
        repository.save(RegistroPeso(gestanteId = request.gestanteId, pesoKg = request.pesoKg))
        return montarGrafico(gestante)
    }

    fun montarGrafico(gestante: Gestante): GraficoPesoResponse {
        val registros = repository.findByGestanteIdOrderByRegistradoEmAsc(gestante.id!!)
        val pontos = registros.map { PontoGraficoPeso(it.registradoEm, it.pesoKg) }

        val alerta = registros.size >= 2 && ganhoExcessivoNoSegundoTrimestre(gestante, registros)
        val mensagem = if (alerta) {
            "Ganho de peso acima de $LIMIAR_GANHO_MENSAL_KG kg/mês detectado no 2º trimestre. " +
                "Recomenda-se orientação nutricional."
        } else null

        return GraficoPesoResponse(
            gestanteId = gestante.id!!,
            pontos = pontos,
            alertaGanhoExcessivo = alerta,
            mensagem = mensagem
        )
    }

    private fun ganhoExcessivoNoSegundoTrimestre(gestante: Gestante, registros: List<RegistroPeso>): Boolean {
        val ultimo = registros[registros.size - 1]
        val penultimo = registros[registros.size - 2]

        val (semanasUltimo, _) = calculadoraGestacional.calcularIdadeGestacional(
            gestante.dataUltimaMenstruacao, ultimo.registradoEm
        )
        if (Trimestre.apartirDaSemana(semanasUltimo.toInt()) != Trimestre.SEGUNDO) {
            return false
        }

        val diasEntreRegistros = ChronoUnit.DAYS.between(penultimo.registradoEm, ultimo.registradoEm)
        if (diasEntreRegistros <= 0) return false

        val ganhoKg = ultimo.pesoKg - penultimo.pesoKg
        val taxaMensal = ganhoKg / diasEntreRegistros * 30
        return taxaMensal > LIMIAR_GANHO_MENSAL_KG
    }
}
