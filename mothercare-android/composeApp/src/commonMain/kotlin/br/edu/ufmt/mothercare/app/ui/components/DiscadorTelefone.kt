package br.edu.ufmt.mothercare.app.ui.components

import androidx.compose.runtime.Composable

/** Devolve a ação que abre o discador com o número informado (ex.: "192"), sem tocar em Context em common. */
@Composable
expect fun rememberAbrirDiscador(): (numero: String) -> Unit
