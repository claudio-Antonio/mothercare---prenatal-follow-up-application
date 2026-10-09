package br.edu.ufmt.mothercare.scheduling.exception

class ConsultaNaoEncontradaException(id: Any) : RuntimeException("Consulta não encontrada: $id")

class ConsultaNaoCancelavelException(status: Any) :
    RuntimeException("Só consultas agendadas podem ser canceladas (status atual: $status)")

class IntervaloConsultaInvalidoException(mensagem: String) : RuntimeException(mensagem)

/** RF05/UC03: agendamento bloqueado por risco obstétrico imediato detectado no último check-in de PA. */
class RiscoObstetricoImediatoException(mensagem: String) : RuntimeException(mensagem)

/** RF03/UC03 A1: horário fora da agenda da clínica ou já ocupado. */
class HorarioIndisponivelException(mensagem: String) : RuntimeException(mensagem)

/** Seção 8.2 (item 6): agendamento de rotina não pode ser marcado após a DPP. */
class AgendamentoAposDppException(dpp: java.time.LocalDate) :
    RuntimeException("Não é possível agendar consulta após a data provável do parto (${dpp}).")
