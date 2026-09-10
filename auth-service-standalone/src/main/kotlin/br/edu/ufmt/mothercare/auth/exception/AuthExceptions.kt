package br.edu.ufmt.mothercare.auth.exception

/** RF01 / A1: "se o email já estiver cadastrado o sistema redireciona para o login". */
class EmailJaCadastradoException(email: String) :
    RuntimeException("Já existe uma conta cadastrada com o email '$email'")

class CredenciaisInvalidasException :
    RuntimeException("Email ou senha inválidos")
