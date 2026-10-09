package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.clinical.entity.ChecklistExameCatalogo
import br.edu.ufmt.mothercare.clinical.entity.DocumentoClinico
import br.edu.ufmt.mothercare.clinical.entity.TipoConteudoArquivo
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.exception.DocumentoNaoEncontradoException
import br.edu.ufmt.mothercare.clinical.exception.FormatoArquivoInvalidoException
import br.edu.ufmt.mothercare.clinical.repository.DocumentoClinicoRepository
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
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
    private val repository: DocumentoClinicoRepository,
    private val exameRepository: ExameLaboratorialRepository
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
                tipoExameRastreado = tipoRastreadoPara(categoria),
                nomeArquivoOriginal = arquivo.originalFilename ?: "documento",
                tipoConteudo = tipoConteudo,
                mimeType = arquivo.contentType,
                tamanhoBytes = arquivo.size,
                conteudo = arquivo.bytes
            )
        )

        return paraResponse(documento)
    }

    @Transactional
    fun excluir(documento: DocumentoClinico) {
        exameRepository.findByDocumentoId(documento.id!!).forEach {
            it.documentoId = null
            exameRepository.save(it)
        }
        repository.delete(documento)
    }

    /**
     * A categoria vem do app (nome do item do checklist ou do tipo de exame).
     * Se corresponder a um exame que o sistema rastreia (Hb, glicemia),
     * guarda o tipo — o checklist passa a contar o laudo como REALIZADO.
     */
    fun tipoRastreadoPara(categoria: String): TipoExame? =
        ChecklistExameCatalogo.porTrimestre.values.flatten()
            .firstOrNull { it.nome.equals(categoria.trim(), ignoreCase = true) }?.tipoRastreadoPeloSistema
            ?: TipoExame.entries.firstOrNull { it.name.equals(categoria.trim(), ignoreCase = true) }

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
