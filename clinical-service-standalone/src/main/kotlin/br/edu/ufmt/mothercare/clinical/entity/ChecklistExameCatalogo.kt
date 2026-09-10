package br.edu.ufmt.mothercare.clinical.entity

/** Catálogo estático do roteiro de exames por trimestre (UC06). */
object ChecklistExameCatalogo {

    val porTrimestre: Map<Trimestre, List<ItemChecklistExame>> = mapOf(
        Trimestre.PRIMEIRO to listOf(
            ItemChecklistExame("Hemograma completo", TipoExame.HEMOGLOBINA),
            ItemChecklistExame("Tipagem sanguínea e fator Rh"),
            ItemChecklistExame("Glicemia de jejum", TipoExame.GLICEMIA_JEJUM),
            ItemChecklistExame("Sorologia para HIV"),
            ItemChecklistExame("Sorologia para Sífilis (VDRL/teste rápido)"),
            ItemChecklistExame("Sorologia para Hepatite B (HBsAg)"),
            ItemChecklistExame("Sorologia para Toxoplasmose (IgG/IgM)"),
            ItemChecklistExame("Exame de urina tipo I e urocultura"),
            ItemChecklistExame("Ultrassonografia obstétrica inicial")
        ),
        Trimestre.SEGUNDO to listOf(
            ItemChecklistExame("Teste Oral de Tolerância à Glicose (TOTG 75g)", TipoExame.GLICEMIA_JEJUM),
            ItemChecklistExame("Coombs indireto (se Rh negativo)"),
            ItemChecklistExame("Ultrassonografia morfológica"),
            ItemChecklistExame("Repetição de sorologia para Toxoplasmose, se IgG não reagente")
        ),
        Trimestre.TERCEIRO to listOf(
            ItemChecklistExame("Hemograma completo (repetição)", TipoExame.HEMOGLOBINA),
            ItemChecklistExame("Sorologias para HIV, Sífilis e Hepatite B (repetição)"),
            ItemChecklistExame("Pesquisa de Estreptococo do grupo B (35ª-37ª semana)"),
            ItemChecklistExame("Ultrassonografia obstétrica de acompanhamento")
        )
    )
}
