package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable
data class ItemChecklistResponse(
    val nome: String,
    val status: String
)

@Serializable
data class ChecklistExamesResponse(
    val gestanteId: String,
    val trimestre: String,
    val itens: List<ItemChecklistResponse>
)
