@file:OptIn(ExperimentalTime::class)

package br.edu.ufmt.mothercare.app.ui.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Dois dígitos com zero à esquerda (ex.: 7 → "07"). */
private fun Int.doisDigitos(): String = toString().padStart(2, '0')

/** Data no formato brasileiro dd/MM/yyyy (formatação manual: não existe DateTimeFormatter em common). */
fun LocalDate.formatarDdMmAaaa(): String = "${day.doisDigitos()}/${(month.ordinal + 1).doisDigitos()}/$year"

/** Data e hora no formato dd/MM/yyyy 'às' HH:mm. */
fun LocalDateTime.formatarDataHoraBr(): String =
    "${date.formatarDdMmAaaa()} às ${hour.doisDigitos()}:${minute.doisDigitos()}"

/** Data e hora atuais no fuso do aparelho. */
fun agoraNoAparelho(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/** Data de hoje no fuso do aparelho. */
fun hojeNoAparelho(): LocalDate = agoraNoAparelho().date

/** Data (em UTC) de um instante em milissegundos, como o DatePicker do Material3 devolve. */
fun dataUtcDeMillis(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

/** Número com uma casa decimal e vírgula, como o pt-BR (ex.: 24.46 → "24,5"); substitui String.format. */
fun Double.formatarUmaCasaDecimal(): String {
    val decimos = kotlin.math.round(this * 10).toLong()
    val sinal = if (decimos < 0) "-" else ""
    val absoluto = kotlin.math.abs(decimos)
    return "$sinal${absoluto / 10},${absoluto % 10}"
}
