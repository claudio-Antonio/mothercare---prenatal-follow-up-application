package br.edu.ufmt.mothercare.clinical.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.util.UUID

/**
 * RN01 / UC02 - Cálculo de IG e DPP (Regra de Naegele).
 *
 * Observação: o exemplo do documento "Regras_de_negócios_tcc.pdf"
 * (DUM 15/11/2025, data atual 18/01/2026) afirma "65 dias / 9 semanas e
 * 2 dias". O cálculo exato de calendário entre essas duas datas é na
 * verdade 64 dias (9 semanas e 1 dia) — há uma diferença de 1 dia no
 * exemplo original do documento. Este teste valida o resultado
 * matematicamente correto (ChronoUnit.DAYS), não o exemplo com o
 * pequeno erro de arredondamento.
 */
class CalculadoraGestacionalServiceTest {

    private val calculadora = CalculadoraGestacionalService()

    @Test
    fun `deve calcular IG conforme exemplo do documento de regras (corrigindo 1 dia de arredondamento)`() {
        val dum = LocalDate.of(2025, 11, 15)
        val dataAtual = LocalDate.of(2026, 1, 18)

        val (semanas, dias) = calculadora.calcularIdadeGestacional(dum, dataAtual)

        assertEquals(9L, semanas)
        assertEquals(1L, dias) // calendário exato: 64 dias = 9 semanas e 1 dia
    }

    @Test
    fun `deve calcular DPP para DUM entre janeiro e marco (regra do README)`() {
        // README: "DUM Jan-Mar: +7 dias ao dia e +9 meses ao mês"
        val dum = LocalDate.of(2026, 2, 10)
        val dpp = calculadora.calcularDataProvavelParto(dum)

        assertEquals(LocalDate.of(2026, 11, 17), dpp)
    }

    @Test
    fun `deve calcular DPP para DUM entre abril e dezembro (regra do README)`() {
        // README: "DUM Abr-Dez: +7 dias, -3 meses, +1 ano" == "+7 dias +9 meses"
        val dum = LocalDate.of(2025, 11, 15)
        val dpp = calculadora.calcularDataProvavelParto(dum)

        assertEquals(LocalDate.of(2026, 8, 22), dpp)
    }

    @ParameterizedTest
    @CsvSource(
        "0, PRIMEIRO", "13, PRIMEIRO",
        "14, SEGUNDO", "27, SEGUNDO",
        "28, TERCEIRO", "41, TERCEIRO"
    )
    fun `deve classificar o trimestre nos limites do documento de regras`(semanas: Int, esperado: Trimestre) {
        assertEquals(esperado, Trimestre.apartirDaSemana(semanas))
    }

    @Test
    fun `deve gerar resposta completa com DPP, trimestre e IMC inicial`() {
        val dum = LocalDate.of(2025, 11, 15)
        val gestante = Gestante(
            id = UUID.randomUUID(), usuarioId = UUID.randomUUID(), dataUltimaMenstruacao = dum,
            pesoInicialKg = 64.0, alturaMetros = 1.60
        )

        val resposta = calculadora.gerarResposta(gestante, dataReferencia = LocalDate.of(2026, 3, 1))

        assertEquals(LocalDate.of(2026, 8, 22), resposta.dataProvavelParto)
        assertEquals(15L, resposta.semanas) // 106 dias = 15 semanas e 1 dia
        assertEquals(1L, resposta.dias)
        assertEquals(Trimestre.SEGUNDO, resposta.trimestre)
        assertEquals(25.0, resposta.imcInicial, 0.001) // 64 / 1,6²
    }

    @Test
    fun `deve rejeitar DUM posterior a data de referencia`() {
        val dumFutura = LocalDate.now().plusDays(1)
        assertThrows(IllegalArgumentException::class.java) {
            calculadora.calcularIdadeGestacional(dumFutura, LocalDate.now())
        }
    }
}
