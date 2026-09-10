package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.ChecklistExamesResponse
import br.edu.ufmt.mothercare.clinical.dto.ItemChecklistResponse
import br.edu.ufmt.mothercare.clinical.entity.ChecklistExameCatalogo
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import org.springframework.stereotype.Service

/**
 * UC06 - Visualizar Checklist de Exames.
 * Filtra a lista de exames recomendados pelo trimestre atual (calculado
 * via UC02) e cruza automaticamente com os registros já lançados no
 * prontuário (UC07) para os tipos que o sistema rastreia hoje
 * (Hemoglobina e Glicemia de Jejum). Os demais itens do roteiro são
 * apresentados como referência informativa ("NAO_RASTREADO"), já que a
 * modelagem de prontuário atual não os digitaliza individualmente.
 */
@Service
class ChecklistExamesService(
    private val exameRepository: ExameLaboratorialRepository,
    private val calculadoraGestacional: CalculadoraGestacionalService
) {

    fun gerarChecklist(gestante: Gestante): ChecklistExamesResponse {
        val (semanas, _) = calculadoraGestacional.calcularIdadeGestacional(gestante.dataUltimaMenstruacao)
        val trimestre = br.edu.ufmt.mothercare.clinical.entity.Trimestre.apartirDaSemana(semanas.toInt())

        val itensCatalogo = ChecklistExameCatalogo.porTrimestre[trimestre].orEmpty()
        val examesRegistrados = exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!)
            .map { it.tipo }
            .toSet()

        val itens = itensCatalogo.map { item ->
            val status = when {
                item.tipoRastreadoPeloSistema == null -> "NAO_RASTREADO"
                item.tipoRastreadoPeloSistema in examesRegistrados -> "REALIZADO"
                else -> "PENDENTE"
            }
            ItemChecklistResponse(nome = item.nome, status = status)
        }

        return ChecklistExamesResponse(
            gestanteId = gestante.id!!,
            trimestre = trimestre,
            itens = itens
        )
    }
}
