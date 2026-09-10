package br.edu.ufmt.mothercare.clinical.entity

/**
 * Item de referência do checklist de exames por trimestre (UC06).
 *
 * A lista abaixo reflete o roteiro de pré-natal de baixo risco
 * amplamente adotado na rede pública brasileira (Caderno de Atenção
 * Básica nº 32, Quadro 12). Como o PDF fornecido ("Regras_de_negócios_
 * tcc.pdf") cita esse quadro como fonte mas não reproduz a lista completa
 * de exames, os nomes abaixo foram preenchidos com o roteiro padrão de
 * pré-natal — **valide item a item contra o Quadro 12 original antes da
 * entrega final**, ajustando `ChecklistExameCatalogo` conforme necessário.
 *
 * [tipoRastreadoPeloSistema] indica se o sistema já possui um
 * [TipoExame] correspondente (hoje: apenas Hemoglobina e Glicemia de
 * Jejum) para cruzar automaticamente com os registros da gestante.
 */
data class ItemChecklistExame(
    val nome: String,
    val tipoRastreadoPeloSistema: TipoExame? = null
)
