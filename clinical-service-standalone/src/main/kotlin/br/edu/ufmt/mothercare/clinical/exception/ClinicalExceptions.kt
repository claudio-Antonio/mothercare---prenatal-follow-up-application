package br.edu.ufmt.mothercare.clinical.exception

class GestanteNaoEncontradaException(id: Any) :
    RuntimeException("Gestante não encontrada: $id")

class DumInvalidaException(mensagem: String) : RuntimeException(mensagem)

class DocumentoNaoEncontradoException(id: Any) :
    RuntimeException("Documento clínico não encontrado: $id")

/** UC07, fluxo alternativo A1: "Se o arquivo não for PDF ou imagem, o sistema solicita reenvio". */
class FormatoArquivoInvalidoException(tipoRecebido: String?) :
    RuntimeException(
        "Formato de arquivo inválido: '${tipoRecebido ?: "desconhecido"}'. " +
            "Apenas PDF (application/pdf) ou imagem (image/jpeg, image/png) são aceitos."
    )
