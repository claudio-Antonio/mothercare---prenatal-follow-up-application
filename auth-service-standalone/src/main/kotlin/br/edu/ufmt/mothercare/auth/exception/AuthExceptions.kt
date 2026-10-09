package br.edu.ufmt.mothercare.auth.exception

/** RF01 / A1: "se o email já estiver cadastrado o sistema redireciona para o login". */
class EmailJaCadastradoException(email: String) :
    RuntimeException("Já existe uma conta cadastrada com o email '$email'")

/** Cadastro público só cria GESTANTE; conta MEDICO exige a chave administrativa. */
class CadastroMedicoNaoAutorizadoException :
    RuntimeException("Cadastro de médico não autorizado")

/** /auth/usuarios/lote: só o painel do médico (via clinical-service, com o token do médico) pode consultar. */
class AcessoNegadoException(mensagem: String) : RuntimeException(mensagem)

class CredenciaisInvalidasException :
    RuntimeException("Email ou senha inválidos")
