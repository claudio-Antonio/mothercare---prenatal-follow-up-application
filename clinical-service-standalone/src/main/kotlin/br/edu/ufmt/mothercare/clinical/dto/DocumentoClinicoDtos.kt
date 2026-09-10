package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.TipoConteudoArquivo
import java.time.Instant
import java.util.UUID

/**
 * UC07 - Gerenciar Prontuário Digital.
 * O upload em si é multipart/form-data (gestanteId + categoria como
 * parâmetros de formulário + o arquivo), então não há um DTO de request
 * em JSON — ver ProntuarioController.
 */
data class DocumentoClinicoResponse(
    val id: UUID,
    val gestanteId: UUID,
    val categoria: String,
    val nomeArquivoOriginal: String,
    val tipoConteudo: TipoConteudoArquivo,
    val tamanhoBytes: Long,
    val enviadoEm: Instant
)
