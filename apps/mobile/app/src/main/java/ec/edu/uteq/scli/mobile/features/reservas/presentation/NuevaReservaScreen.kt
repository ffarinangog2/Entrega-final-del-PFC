package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaReservaScreen(viewModel: NuevaReservaViewModel) {
    val state by viewModel.uiState.collectAsState()
    val rango = state.rangoPeriodo
    var mostrarDatePicker by remember { mutableStateOf(false) }

    val fechaParsed = parsearFechaSegura(state.fechaReserva)
    val fechaValida = rango is PeriodoReservaRango.Valido &&
        fechaParsed != null &&
        !fechaParsed.isBefore(rango.fechaMinima) &&
        !fechaParsed.isAfter(rango.fechaMaxima)

    val puedeEnviar = !state.enviando &&
        !state.cargandoCatalogos &&
        fechaValida &&
        state.periodo != null &&
        listOf(state.docenteId, state.materiaId, state.laboratorioId, state.horaInicio, state.horaFin, state.motivo).none(String::isBlank) &&
        (state.numeroParticipantes.toIntOrNull() ?: 0) > 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("nueva_solicitud_form"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Nueva solicitud", style = MaterialTheme.typography.headlineSmall)
        if (state.cargandoCatalogos) CircularProgressIndicator()
        Text("Docente: ${state.docenteCodigo.ifBlank { "Cargando…" }}")
        Selector("Materia", state.materias.map { it.id to "${it.codigo} — ${it.nombre}" }, state.materiaId) { id -> viewModel.actualizar { it.copy(materiaId = id) } }
        Text("Período: ${state.periodo?.let { "${it.codigo} — ${it.nombre}" } ?: "No disponible"}")

        if (rango is PeriodoReservaRango.Invalido) {
            Text(
                text = rango.mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("mensaje_periodo_invalido")
            )
        }

        Selector("Laboratorio", state.laboratorios.map { it.id to "${it.codigo} — ${it.nombre}" }, state.laboratorioId) { id -> viewModel.actualizar { it.copy(laboratorioId = id) } }

        CampoFecha(
            fecha = state.fechaReserva,
            habilitado = rango is PeriodoReservaRango.Valido,
            onClick = { mostrarDatePicker = true }
        )

        Campo("Hora inicio (HH:mm)", state.horaInicio) { value -> viewModel.actualizar { it.copy(horaInicio = value) } }
        Campo("Hora fin (HH:mm)", state.horaFin) { value -> viewModel.actualizar { it.copy(horaFin = value) } }
        Campo("Participantes", state.numeroParticipantes) { value -> viewModel.actualizar { it.copy(numeroParticipantes = value) } }
        Campo("Motivo", state.motivo) { value -> viewModel.actualizar { it.copy(motivo = value) } }
        Campo("Observación (opcional)", state.observacion) { value -> viewModel.actualizar { it.copy(observacion = value) } }

        Button(
            onClick = viewModel::comprobarDisponibilidad,
            enabled = !state.comprobando && listOf(state.laboratorioId, state.fechaReserva, state.horaInicio, state.horaFin).none(String::isBlank),
            modifier = Modifier.fillMaxWidth().testTag("comprobar_disponibilidad")
        ) {
            Text("Comprobar disponibilidad")
        }

        state.disponible?.let { Text(if (it) "Disponible" else "No disponible", modifier = Modifier.testTag("resultado_disponibilidad")) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("nueva_solicitud_error")) }
        state.solicitudCreada?.let { Text("Solicitud creada correctamente — ${it.estado}", modifier = Modifier.testTag("nueva_solicitud_exito")) }

        Button(
            onClick = viewModel::enviar,
            enabled = puedeEnviar,
            modifier = Modifier.fillMaxWidth().testTag("enviar_solicitud")
        ) {
            Text(if (state.enviando) "Enviando…" else "Enviar solicitud")
        }
    }

    if (mostrarDatePicker && rango is PeriodoReservaRango.Valido) {
        val selectableDates = remember(rango) {
            ReservaSelectableDates(rango.fechaMinima, rango.fechaMaxima)
        }
        val initialMillis = remember(state.fechaReserva, rango) {
            fechaParsed?.let { localDateToUtcMillis(it) } ?: localDateToUtcMillis(rango.fechaMinima)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val localDate = utcMillisToLocalDate(selectedMillis)
                            viewModel.actualizar { it.copy(fechaReserva = localDate.toString()) }
                        }
                        mostrarDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                    modifier = Modifier.testTag("confirmar_fecha")
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDatePicker = false },
                    modifier = Modifier.testTag("cancelar_fecha")
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CampoFecha(
    fecha: String,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fecha,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text("Fecha (AAAA-MM-DD)") },
            placeholder = { Text("Seleccionar fecha") },
            trailingIcon = {
                IconButton(
                    onClick = onClick,
                    enabled = habilitado,
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Seleccionar fecha"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("campo_fecha_reserva")
        )
        if (habilitado) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onClick)
            )
        }
    }
}

@Composable
private fun Selector(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(options.firstOrNull { it.first == selected }?.second ?: "Seleccionar $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option.second) }, onClick = { onSelect(option.first); expanded = false }) }
        }
    }
}

@Composable
private fun Campo(label: String, value: String, onValueChange: (String) -> Unit) =
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
