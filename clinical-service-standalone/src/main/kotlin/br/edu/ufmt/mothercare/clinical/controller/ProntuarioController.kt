package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.clinical.entity.TipoConteudoArquivo
import br.edu.ufmt.mothercare.clinical.service.ProntuarioDocumentoService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * UC07 - Gerenciar Prontuário Digital.
 * Endpoints de upload, listagem cronológica e download dos laudos.
 */
@RestController
@RequestMapping("/prontuario/documentos")
class ProntuarioController(
    private val service: ProntuarioDocumentoService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun enviar(
        @RequestParam gestanteId: UUID,
        @RequestParam categoria: String,
        @RequestPart arquivo: MultipartFile
    ): ResponseEntity<DocumentoClinicoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.enviar(gestanteId, categoria, arquivo))

    @GetMapping("/{gestanteId}")
    fun listar(@PathVariable gestanteId: UUID): ResponseEntity<List<DocumentoClinicoResponse>> =
        ResponseEntity.ok(service.listarPorGestante(gestanteId))

    @GetMapping("/conteudo/{id}")
    fun baixar(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val documento = service.buscarOuFalhar(id)
        val mediaType = when (documento.tipoConteudo) {
            TipoConteudoArquivo.PDF -> MediaType.APPLICATION_PDF
            TipoConteudoArquivo.IMAGEM -> MediaType.IMAGE_JPEG
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"${documento.nomeArquivoOriginal}\"")
            .contentType(mediaType)
            .body(documento.conteudo)
    }
}
