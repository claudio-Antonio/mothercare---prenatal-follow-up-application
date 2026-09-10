package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.clinical.entity.DocumentoClinico
import br.edu.ufmt.mothercare.clinical.entity.TipoConteudoArquivo
import br.edu.ufmt.mothercare.clinical.exception.DocumentoNaoEncontradoException
import br.edu.ufmt.mothercare.clinical.exception.FormatoArquivoInvalidoException
import br.edu.ufmt.mothercare.clinical.repository.DocumentoClinicoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * UC07 - Gerenciar Prontuário Digital / RF07.
 *
 * Fluxo alternativo A1 do UC07: "Se o arquivo não for PDF ou imagem, o
 * sistema solicita reenvio" -> validado pelo Content-Type do multipart
 * antes de qualquer persistência.
 *
 * O binário é salvo via @Lob (bytea) no mesmo Postgres do clinical-service
 * (Database per Service, Seção 4.3.3 do TCC) — ver observação de trade-off
 * de escalabilidade no Javadoc da entidade DocumentoClinico.
 */
@Service
class ProntuarioDocumentoService(
    private val repository: DocumentoClinicoRepository
) {
    companion object {
        private val CONTENT_TYPES_ACEITOS = mapOf(
            "application/pdf" to TipoConteudoArquivo.PDF,
            "image/jpeg" to TipoConteudoArquivo.IMAGEM,
            "image/png" to TipoConteudoArquivo.IMAGEM
        )
    }

    @Transactional
    fun enviar(gestanteId: UUID, categoria: String, arquivo: MultipartFile): DocumentoClinicoResponse {
        val tipoConteudo = CONTENT_TYPES_ACEITOS[arquivo.contentType]
            ?: throw FormatoArquivoInvalidoException(arquivo.contentType)

        val documento = repository.save(
            DocumentoClinico(
                gestanteId = gestanteId,
                categoria = categoria,
                nomeArquivoOriginal = arquivo.originalFilename ?: "documento",
                tipoConteudo = tipoConteudo,
                tamanhoBytes = arquivo.size,
                conteudo = arquivo.bytes
            )
        )

        return paraResponse(documento)
    }

    /** Doc. de regras de negócio: listagem "em ordem cronológica reversa". */
    fun listarPorGestante(gestanteId: UUID): List<DocumentoClinicoResponse> =
        repository.findByGestanteIdOrderByEnviadoEmDesc(gestanteId).map { paraResponse(it) }

    fun buscarOuFalhar(id: UUID): DocumentoClinico =
        repository.findById(id).orElseThrow { DocumentoNaoEncontradoException(id) }

    private fun paraResponse(documento: DocumentoClinico) = DocumentoClinicoResponse(
        id = documento.id!!,
        gestanteId = documento.gestanteId,
        categoria = documento.categoria,
        nomeArquivoOriginal = documento.nomeArquivoOriginal,
        tipoConteudo = documento.tipoConteudo,
        tamanhoBytes = documento.tamanhoBytes,
        enviadoEm = documento.enviadoEm
    )
}
