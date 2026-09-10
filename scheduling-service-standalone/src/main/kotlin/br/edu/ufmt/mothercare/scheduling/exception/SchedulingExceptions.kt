package br.edu.ufmt.mothercare.scheduling.exception

class IntervaloConsultaInvalidoException(mensagem: String) : RuntimeException(mensagem)

/** RF05/UC03: agendamento bloqueado por risco obstétrico imediato detectado no último check-in de PA. */
class RiscoObstetricoImediatoException(mensagem: String) : RuntimeException(mensagem)
