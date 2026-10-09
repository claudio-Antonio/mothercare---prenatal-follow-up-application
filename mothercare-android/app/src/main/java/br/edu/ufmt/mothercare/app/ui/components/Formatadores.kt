package br.edu.ufmt.mothercare.app.ui.components

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_DATA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")

/**
 * Aceita os dois formatos que a API devolve: Instant em UTC ("...Z", ex.: registradoEm)
 * — convertido para o fuso do aparelho — e LocalDateTime sem fuso (ex.: dataHora da consulta).
 */
fun formatarDataHoraApi(valor: String): String =
    runCatching { Instant.parse(valor).atZone(ZoneId.systemDefault()).toLocalDateTime().format(FORMATO_DATA_HORA) }
        .recoverCatching { LocalDateTime.parse(valor).format(FORMATO_DATA_HORA) }
        .getOrDefault(valor)

fun dataHoraApiNoFuturo(valor: String): Boolean =
    runCatching { LocalDateTime.parse(valor).isAfter(LocalDateTime.now()) }.getOrDefault(false)

/** Enum NivelRisco do backend → texto para a usuária. */
fun rotuloRisco(nivel: String): String = when (nivel) {
    "NORMAL" -> "Normal"
    "ATENCAO" -> "Atenção"
    "CRITICO" -> "Crítica"
    else -> nivel
}

fun rotuloPeriodicidade(periodicidade: String): String = when (periodicidade) {
    "MENSAL" -> "Mensal (até a 27ª semana)"
    "SEMANAL" -> "Semanal (a partir da 28ª semana)"
    else -> periodicidade
}

fun rotuloStatusConsulta(status: String): String = when (status) {
    "AGENDADA" -> "Agendada"
    "CONCLUIDA" -> "Realizada"
    "CANCELADA" -> "Cancelada"
    else -> status
}

fun rotuloTrimestre(trimestre: String): String = when (trimestre) {
    "PRIMEIRO" -> "1º trimestre"
    "SEGUNDO" -> "2º trimestre"
    "TERCEIRO" -> "3º trimestre"
    else -> trimestre
}

/** Documento de regras de negócio: mínimo de 6 consultas de pré-natal (mesmo valor do scheduling-service). */
const val MINIMO_CONSULTAS_PRE_NATAL = 6
