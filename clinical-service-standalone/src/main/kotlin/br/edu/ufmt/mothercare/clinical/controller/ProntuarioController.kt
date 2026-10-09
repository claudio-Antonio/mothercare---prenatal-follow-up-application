package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.clinical.service.ControleAcessoService
import br.edu.ufmt.mothercare.clinical.service.ProntuarioDocumentoService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * UC07 - Gerenciar Prontuário Digital.
 * Endpoints de upload, listagem cronológica e download dos laudos.
 */
@RestController
@RequestMapping("/prontuario/documentos")
class ProntuarioController(
    private val service: ProntuarioDocumentoService,
    private val controleAcesso: ControleAcessoService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun enviar(
        @RequestParam gestanteId: UUID,
        @RequestParam categoria: String,
        @RequestPart arquivo: MultipartFile
    ): ResponseEntity<DocumentoClinicoResponse> {
        controleAcesso.verificarRegistroClinico(gestanteId)
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviar(gestanteId, categoria, arquivo))
    }

    @GetMapping("/{gestanteId}")
    fun listar(@PathVariable gestanteId: UUID): ResponseEntity<List<DocumentoClinicoResponse>> {
        controleAcesso.verificarAcesso(gestanteId)
        return ResponseEntity.ok(service.listarPorGestante(gestanteId))
    }

    @GetMapping("/conteudo/{id}")
    fun baixar(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val documento = service.buscarOuFalhar(id)
        controleAcesso.verificarAcesso(documento.gestanteId)
        // ContentDisposition escapa aspas e codifica (RFC 5987) nomes com acento, ex.: "ultrassonografia_1º_tri.pdf".
        val disposition = ContentDisposition.attachment()
            .filename(documento.nomeArquivoOriginal, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(documento.mimeTypeDownload()))
            .body(documento.conteudo)
    }

    /** Laudo enviado por engano. Exames que apontavam para ele ficam sem laudo (documentoId = null). */
    @DeleteMapping("/{id}")
    fun excluir(@PathVariable id: UUID): ResponseEntity<Void> {
        val documento = service.buscarOuFalhar(id)
        controleAcesso.verificarRegistroClinico(documento.gestanteId)
        service.excluir(documento)
        return ResponseEntity.noContent().build()
    }
}
