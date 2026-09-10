package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import java.util.UUID

/** UC06 - Visualizar Checklist de Exames. */
data class ItemChecklistResponse(
    val nome: String,
    val status: String // "REALIZADO" | "PENDENTE" | "NAO_RASTREADO"
)

data class ChecklistExamesResponse(
    val gestanteId: UUID,
    val trimestre: Trimestre,
    val itens: List<ItemChecklistResponse>
)
