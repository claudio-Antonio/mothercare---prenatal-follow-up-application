package br.edu.ufmt.mothercare.clinical.exception

class GestanteNaoEncontradaException(id: Any) :
    RuntimeException("Gestante não encontrada: $id")

/** A coluna usuarioId é unique: sem esta checagem o insert duplicado viraria 500. */
class GestanteJaCadastradaException(usuarioId: Any) :
    RuntimeException("Já existe um cadastro clínico para o usuário $usuarioId")

/** Gestante pertence a outro usuário (ou a outro médico). */
class AcessoNegadoException(mensagem: String = "Você não tem acesso aos dados desta gestante") :
    RuntimeException(mensagem)

class GestanteJaVinculadaException(gestanteId: Any) :
    RuntimeException("A gestante $gestanteId já está vinculada a outro médico")

class DumInvalidaException(mensagem: String) : RuntimeException(mensagem)

class DocumentoNaoEncontradoException(id: Any) :
    RuntimeException("Documento clínico não encontrado: $id")

/** UC07, fluxo alternativo A1: "Se o arquivo não for PDF ou imagem, o sistema solicita reenvio". */
class FormatoArquivoInvalidoException(tipoRecebido: String?) :
    RuntimeException(
        "Formato de arquivo inválido: '${tipoRecebido ?: "desconhecido"}'. " +
            "Apenas PDF (application/pdf) ou imagem (image/jpeg, image/png) são aceitos."
    )

/** RF08: notificação inexistente ou que pertence a outro usuário (404 para não revelar a existência). */
class NotificacaoNaoEncontradaException(id: Any) :
    RuntimeException("Notificação não encontrada: $id")
