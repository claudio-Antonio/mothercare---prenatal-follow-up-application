package br.edu.ufmt.mothercare.clinical.entity

/**
 * Trimestres gestacionais conforme Documento de Regras de Negócio:
 * 1º até a 13ª semana; 2º da 14ª à 27ª; 3º da 28ª semana até o parto.
 */
enum class Trimestre {
    PRIMEIRO,
    SEGUNDO,
    TERCEIRO;

    companion object {
        fun apartirDaSemana(semanas: Int): Trimestre = when {
            semanas <= 13 -> PRIMEIRO
            semanas <= 27 -> SEGUNDO
            else -> TERCEIRO
        }
    }
}
