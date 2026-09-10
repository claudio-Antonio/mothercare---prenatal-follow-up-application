package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.exception.GestanteNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** UC01 + UC02 - Cadastro inicial e disponibilização da IG/DPP calculadas. */
@Service
class GestanteService(
    private val repository: GestanteRepository,
    private val calculadora: CalculadoraGestacionalService
) {

    @Transactional
    fun cadastrar(request: CadastroGestanteRequest): IdadeGestacionalResponse {
        val gestante = repository.save(
            Gestante(
                usuarioId = request.usuarioId,
                dataUltimaMenstruacao = request.dataUltimaMenstruacao,
                pesoInicialKg = request.pesoInicialKg,
                alturaMetros = request.alturaMetros
            )
        )
        return calculadora.gerarResposta(gestante)
    }

    fun buscarOuFalhar(id: UUID): Gestante =
        repository.findById(id).orElseThrow { GestanteNaoEncontradaException(id) }

    fun obterIdadeGestacional(id: UUID): IdadeGestacionalResponse =
        calculadora.gerarResposta(buscarOuFalhar(id))
}
