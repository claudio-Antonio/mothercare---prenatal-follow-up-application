package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.ChecklistExamesResponse
import br.edu.ufmt.mothercare.clinical.dto.ItemChecklistResponse
import br.edu.ufmt.mothercare.clinical.entity.ChecklistExameCatalogo
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.repository.DocumentoClinicoRepository
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import org.springframework.stereotype.Service
import java.time.ZoneId

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
    private val calculadoraGestacional: CalculadoraGestacionalService,
    private val documentoRepository: DocumentoClinicoRepository
) {

    fun gerarChecklist(gestante: Gestante): ChecklistExamesResponse {
        val (semanas, _) = calculadoraGestacional.calcularIdadeGestacional(gestante.dataUltimaMenstruacao)
        val trimestre = br.edu.ufmt.mothercare.clinical.entity.Trimestre.apartirDaSemana(semanas.toInt())

        val itensCatalogo = ChecklistExameCatalogo.porTrimestre[trimestre].orEmpty()
        // Só conta exame feito no trimestre atual: a "repetição" do hemograma no
        // 3º trimestre não pode aparecer REALIZADO por causa do exame do 1º.
        val inicioTrimestre = gestante.dataUltimaMenstruacao.plusWeeks(trimestre.semanaInicial.toLong())
        val examesRegistrados = exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!)
            .filter { !it.registradoEm.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(inicioTrimestre) }
            .map { it.tipo }
            .toSet()
        // Laudos enviados no trimestre: o nome do item como categoria marca o item como REALIZADO.
        val laudos = documentoRepository.findResumoByGestanteId(gestante.id!!)
            .filter { !it.enviadoEm.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(inicioTrimestre) }
        val categoriasComLaudo = laudos.map { it.categoria.trim().lowercase() }.toSet()
        val tiposComLaudo = laudos.mapNotNull { it.tipoExameRastreado }.toSet()

        val itens = itensCatalogo.map { item ->
            val tipo = item.tipoRastreadoPeloSistema
            val status = when {
                item.nome.lowercase() in categoriasComLaudo -> "REALIZADO"
                tipo == null -> "NAO_RASTREADO"
                tipo in examesRegistrados || tipo in tiposComLaudo -> "REALIZADO"
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
