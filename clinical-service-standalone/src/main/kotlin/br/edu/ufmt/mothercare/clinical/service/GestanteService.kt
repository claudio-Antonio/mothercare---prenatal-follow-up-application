package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.client.AuthServiceClient
import br.edu.ufmt.mothercare.clinical.dto.AtualizarGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.GestanteResumoResponse
import br.edu.ufmt.mothercare.clinical.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.exception.GestanteJaCadastradaException
import br.edu.ufmt.mothercare.clinical.exception.GestanteJaVinculadaException
import br.edu.ufmt.mothercare.clinical.exception.GestanteNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GestanteService(
    private val repository: GestanteRepository,
    private val calculadora: CalculadoraGestacionalService,
    private val triagemHipertensivaService: TriagemHipertensivaService,
    private val exameLaboratorialRepository: ExameLaboratorialRepository,
    private val authServiceClient: AuthServiceClient
) {
    private val log = LoggerFactory.getLogger(GestanteService::class.java)

    @Transactional
    fun cadastrar(request: CadastroGestanteRequest): IdadeGestacionalResponse {
        if (repository.existsByUsuarioId(request.usuarioId)) {
            throw GestanteJaCadastradaException(request.usuarioId)
        }
        val gestante = repository.save(
            Gestante(
                usuarioId = request.usuarioId,
                dataUltimaMenstruacao = request.dataUltimaMenstruacao,
                pesoInicialKg = request.pesoInicialKg,
                alturaMetros = request.alturaMetros,
                medicoId = request.medicoId
            )
        )
        return calculadora.gerarResposta(gestante)
    }

    fun buscarOuFalhar(id: UUID): Gestante =
        repository.findById(id).orElseThrow { GestanteNaoEncontradaException(id) }

    fun obterIdadeGestacional(id: UUID): IdadeGestacionalResponse =
        calculadora.gerarResposta(buscarOuFalhar(id))

    fun buscarPorUsuario(usuarioId: UUID): IdadeGestacionalResponse =
        calculadora.gerarResposta(buscarPorUsuarioOuFalhar(usuarioId))

    @Transactional
    fun atualizar(usuarioId: UUID, request: AtualizarGestanteRequest): IdadeGestacionalResponse {
        val gestante = buscarPorUsuarioOuFalhar(usuarioId)
        gestante.dataUltimaMenstruacao = request.dataUltimaMenstruacao
        gestante.pesoInicialKg = request.pesoInicialKg
        gestante.alturaMetros = request.alturaMetros
        return calculadora.gerarResposta(repository.save(gestante))
    }

    /** A gestante remove o vínculo atual — assim pode passar o código a outro médico. */
    @Transactional
    fun desvincularMedico(usuarioId: UUID) {
        val gestante = buscarPorUsuarioOuFalhar(usuarioId)
        if (gestante.medicoId == null) return
        gestante.medicoId = null
        repository.save(gestante)
    }

    private fun buscarPorUsuarioOuFalhar(usuarioId: UUID): Gestante =
        repository.findByUsuarioId(usuarioId) ?: throw GestanteNaoEncontradaException("usuário $usuarioId")

    /** Idempotente para o mesmo médico; não "rouba" gestante já vinculada a outro. */
    @Transactional
    fun vincularMedico(gestanteId: UUID, medicoId: UUID) {
        val gestante = buscarOuFalhar(gestanteId)
        val medicoAtual = gestante.medicoId
        if (medicoAtual == medicoId) return
        if (medicoAtual != null) throw GestanteJaVinculadaException(gestanteId)
        gestante.medicoId = medicoId
        repository.save(gestante)
    }

    fun listarPorMedico(medicoId: UUID, authorizationHeader: String): List<GestanteResumoResponse> {
        val gestantes = repository.findByMedicoId(medicoId)

        val nomesPorUsuarioId = try {
            authServiceClient.buscarNomes(gestantes.map { it.usuarioId }, authorizationHeader)
        } catch (ex: Exception) {
            log.warn("Falha ao buscar nomes no auth-service, painel do médico seguirá sem nome: {}", ex.message)
            emptyMap()
        }

        return gestantes.map { gestante ->
            val (semanas, _) = calculadora.calcularIdadeGestacional(gestante.dataUltimaMenstruacao)
            val statusPA = triagemHipertensivaService.obterUltimoStatus(gestante.id!!)

            GestanteResumoResponse(
                gestanteId = gestante.id!!,
                nomeGestante = nomesPorUsuarioId[gestante.usuarioId],
                semanas = semanas,
                ultimoNivelRiscoPA = statusPA.nivelRisco,
                possuiAlertaLaboratorial = possuiAlertaLaboratorialAtivo(gestante.id!!)
            )
        }
    }

    private fun possuiAlertaLaboratorialAtivo(gestanteId: UUID): Boolean {
        val exames = exameLaboratorialRepository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId)
        val ultimoPorTipo = exames.groupBy { it.tipo }.mapValues { it.value.first() }
        return ultimoPorTipo.values.any { it.alertaDisparado }
    }
}