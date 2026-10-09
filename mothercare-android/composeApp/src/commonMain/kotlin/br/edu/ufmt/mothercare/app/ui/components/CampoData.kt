package br.edu.ufmt.mothercare.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

/** Formata yyyy-MM-dd (formato da API) como dd/MM/yyyy; devolve o texto original se não for uma data. */
fun formatarDataApi(valor: String): String =
    runCatching { LocalDate.parse(valor.take(10)).formatarDdMmAaaa() }.getOrDefault(valor)

/**
 * Campo somente-leitura que abre um DatePicker. [valorIso] e o retorno de
 * [onDataSelecionada] estão no formato da API (yyyy-MM-dd).
 * [permitir] filtra as datas selecionáveis (ex.: DUM não pode ser futura).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(
    valorIso: String,
    rotulo: String,
    onDataSelecionada: (String) -> Unit,
    permitir: (LocalDate) -> Boolean = { true }
) {
    var mostrarSeletor by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = if (valorIso.isBlank()) "" else formatarDataApi(valorIso),
        onValueChange = {},
        label = { Text(rotulo) },
        placeholder = { Text("Toque para selecionar") },
        readOnly = true,
        enabled = false,
        modifier = Modifier.fillMaxWidth().clickable { mostrarSeletor = true },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    if (mostrarSeletor) {
        // O DatePicker trabalha com meia-noite em UTC, por isso a conversão usa UTC.
        val estado = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    permitir(dataUtcDeMillis(utcTimeMillis))
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSeletor = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        onDataSelecionada(dataUtcDeMillis(it).toString())
                    }
                    mostrarSeletor = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostrarSeletor = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}
