package ec.edu.uteq.scli.mobile.features.incidentes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.R
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.Prioridad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentesScreen(viewModel: IncidentesViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.incidentes_form_titulo))

            OutlinedTextField(
                value = uiState.laboratorioEquipo,
                onValueChange = viewModel::onLaboratorioEquipoChange,
                label = { Text(stringResource(R.string.incidentes_form_laboratorio_equipo)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text(stringResource(R.string.incidentes_form_descripcion)) },
                modifier = Modifier.fillMaxWidth(),
            )

            PrioridadDropdown(
                seleccionada = uiState.prioridad,
                onSeleccionar = viewModel::onPrioridadChange,
            )

            FechaSelector(
                fechaMillis = uiState.fechaMillis,
                onFechaSeleccionada = viewModel::onFechaChange,
            )

            if (uiState.error != null) {
                Text(text = stringResource(R.string.incidentes_form_error_campos))
            }

            Button(
                onClick = viewModel::onGuardarIncidente,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.incidentes_form_guardar))
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.incidentes_titulo),
            modifier = Modifier.padding(16.dp),
        )

        if (uiState.incidentes.isEmpty()) {
            Text(
                text = stringResource(R.string.incidentes_lista_vacia),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.incidentes, key = { it.id }) { incidente ->
                    IncidenteItem(incidente)
                }
            }
        }
    }
}

@Composable
private fun IncidenteItem(incidente: Incidente) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = incidente.laboratorioEquipo)
            Text(text = incidente.descripcion)
            Text(text = "${incidente.prioridad} · ${formatearFecha(incidente.fechaMillis)}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrioridadDropdown(
    seleccionada: Prioridad,
    onSeleccionar: (Prioridad) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
    ) {
        OutlinedTextField(
            value = seleccionada.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.incidentes_form_prioridad)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
        ) {
            Prioridad.entries.forEach { prioridad ->
                DropdownMenuItem(
                    text = { Text(prioridad.name) },
                    onClick = {
                        onSeleccionar(prioridad)
                        expandido = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FechaSelector(
    fechaMillis: Long,
    onFechaSeleccionada: (Long) -> Unit,
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { mostrarDialogo = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("${stringResource(R.string.incidentes_form_fecha)}: ${formatearFecha(fechaMillis)}")
    }

    if (mostrarDialogo) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaMillis)

        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onFechaSeleccionada)
                    mostrarDialogo = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatearFecha(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
