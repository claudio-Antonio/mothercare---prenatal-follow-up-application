package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.entity.DocumentoClinico
import br.edu.ufmt.mothercare.clinical.entity.TipoConteudoArquivo
import br.edu.ufmt.mothercare.clinical.exception.DocumentoNaoEncontradoException
import br.edu.ufmt.mothercare.clinical.exception.FormatoArquivoInvalidoException
import br.edu.ufmt.mothercare.clinical.repository.DocumentoClinicoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional
import java.util.UUID

/** UC07 - Gerenciar Prontuário Digital, incluindo o fluxo alternativo A1 (formato inválido). */
class ProntuarioDocumentoServiceTest {

    private val repository = mockk<DocumentoClinicoRepository>()
    private val service = ProntuarioDocumentoService(repository)
    private val gestanteId = UUID.randomUUID()

    private fun mockSave() {
        val slot = slot<DocumentoClinico>()
        every { repository.save(capture(slot)) } answers {
            DocumentoClinico(
                id = UUID.randomUUID(),
                gestanteId = slot.captured.gestanteId,
                categoria = slot.captured.categoria,
                nomeArquivoOriginal = slot.captured.nomeArquivoOriginal,
                tipoConteudo = slot.captured.tipoConteudo,
                tamanhoBytes = slot.captured.tamanhoBytes,
                conteudo = slot.captured.conteudo
            )
        }
    }

    @Test
    fun `deve aceitar upload de PDF`() {
        mockSave()
        val arquivo = MockMultipartFile("arquivo", "laudo.pdf", "application/pdf", "conteudo-fake".toByteArray())

        val response = service.enviar(gestanteId, "Hemograma completo", arquivo)

        assertEquals(TipoConteudoArquivo.PDF, response.tipoConteudo)
        assertEquals("laudo.pdf", response.nomeArquivoOriginal)
    }

    @Test
    fun `deve aceitar upload de imagem JPEG`() {
        mockSave()
        val arquivo = MockMultipartFile("arquivo", "laudo.jpg", "image/jpeg", "conteudo-fake".toByteArray())

        val response = service.enviar(gestanteId, "Ultrassonografia obstétrica inicial", arquivo)

        assertEquals(TipoConteudoArquivo.IMAGEM, response.tipoConteudo)
    }

    @Test
    fun `deve rejeitar formato nao suportado (UC07 - A1)`() {
        val arquivo = MockMultipartFile("arquivo", "laudo.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "conteudo-fake".toByteArray())

        assertThrows(FormatoArquivoInvalidoException::class.java) {
            service.enviar(gestanteId, "Hemograma completo", arquivo)
        }
    }

    @Test
    fun `deve lancar excecao ao buscar documento inexistente`() {
        val idInexistente = UUID.randomUUID()
        every { repository.findById(idInexistente) } returns Optional.empty()

        assertThrows(DocumentoNaoEncontradoException::class.java) {
            service.buscarOuFalhar(idInexistente)
        }
    }

    @Test
    fun `deve listar documentos da gestante em ordem cronologica reversa`() {
        val doc1 = DocumentoClinico(
            id = UUID.randomUUID(), gestanteId = gestanteId, categoria = "Hemograma completo",
            nomeArquivoOriginal = "laudo1.pdf", tipoConteudo = TipoConteudoArquivo.PDF,
            tamanhoBytes = 10, conteudo = "a".toByteArray()
        )
        every { repository.findByGestanteIdOrderByEnviadoEmDesc(gestanteId) } returns listOf(doc1)

        val documentos = service.listarPorGestante(gestanteId)

        assertEquals(1, documentos.size)
        assertEquals("Hemograma completo", documentos[0].categoria)
    }
}
