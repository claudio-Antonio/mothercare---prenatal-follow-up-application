package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

/** UC04: valores de pressão arterial informados no check-in. */
data class CheckInPressaoRequest(
    @field:NotNull
    val gestanteId: UUID,

    @field:Min(40) @field:Max(260)
    val sistolica: Int,

    @field:Min(20) @field:Max(200)
    val diastolica: Int
)

/**
 * RN02 / UC05 / UC11: resultado da triagem hipertensiva.
 * [agendamentoLiberado] = false sinaliza ao scheduling-service (ou ao
 * app) que o fluxo de agendamento eletivo deve ser bloqueado (UC05).
 */
data class ResultadoTriagemPressaoResponse(
    val nivelRisco: NivelRisco,
    val agendamentoLiberado: Boolean,
    val mensagem: String
)

/**
 * RF04/RF05 — consultado pelo scheduling-service antes de confirmar um
 * agendamento (UC03, passo 3: "invoca obrigatoriamente" a triagem).
 * Se a gestante nunca fez check-in, [nivelRisco] volta NORMAL por
 * padrão — não há motivo pra bloquear agendamento por falta de dado.
 */
data class UltimoStatusPressaoResponse(
    val gestanteId: UUID,
    val nivelRisco: NivelRisco,
    val registradoEm: java.time.Instant?
)
