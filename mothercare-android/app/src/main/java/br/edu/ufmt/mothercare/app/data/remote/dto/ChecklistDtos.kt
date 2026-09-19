package br.edu.ufmt.mothercare.app.data.remote.dto

data class ItemChecklistResponse(
    val nome: String,
    val status: String
)

data class ChecklistExamesResponse(
    val gestanteId: String,
    val trimestre: String,
    val itens: List<ItemChecklistResponse>
)
