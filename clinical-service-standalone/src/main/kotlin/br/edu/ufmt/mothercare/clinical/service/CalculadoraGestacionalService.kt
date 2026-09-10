package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * RN01 / UC02 - Calcular Idade Gestacional (IG) e Data Provável do Parto (DPP).
 *
 * Regra de Naegele (DPP): DUM + 7 dias - 3 meses + 1 ano.
 * Matematicamente isso equivale a "DUM + 7 dias + 9 meses" em qualquer mês
 * do ano — é a mesma regra descrita de duas formas no README (separando
 * DUM de Jan-Mar de DUM Abr-Dez) e na Tabela 2 do TCC ("+7 dias -3 meses
 * +1 ano"). Usar `plusMonths(9)` evita a bifurcação manual por mês e deixa
 * o `java.time` cuidar da virada de ano/dias de forma correta.
 *
 * IG: número de semanas e dias entre a DUM e a data atual
 * (IG = (DataAtual - DUM) / 7), calculado por diferença exata de dias
 * (ChronoUnit.DAYS), não por contagem aproximada de meses.
 */
@Service
class CalculadoraGestacionalService {

    fun calcularDataProvavelParto(dum: LocalDate): LocalDate =
        dum.plusDays(7).plusMonths(9)

    fun calcularIdadeGestacional(dum: LocalDate, dataReferencia: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val diasDecorridos = ChronoUnit.DAYS.between(dum, dataReferencia)
        require(diasDecorridos >= 0) { "A DUM não pode ser posterior à data de referência" }
        val semanas = diasDecorridos / 7
        val dias = diasDecorridos % 7
        return semanas to dias
    }

    fun gerarResposta(gestante: Gestante, dataReferencia: LocalDate = LocalDate.now()): IdadeGestacionalResponse {
        val (semanas, dias) = calcularIdadeGestacional(gestante.dataUltimaMenstruacao, dataReferencia)
        return IdadeGestacionalResponse(
            gestanteId = gestante.id!!,
            dataUltimaMenstruacao = gestante.dataUltimaMenstruacao,
            dataProvavelParto = calcularDataProvavelParto(gestante.dataUltimaMenstruacao),
            semanas = semanas,
            dias = dias,
            trimestre = Trimestre.apartirDaSemana(semanas.toInt()),
            imcInicial = gestante.imcInicial()
        )
    }
}
