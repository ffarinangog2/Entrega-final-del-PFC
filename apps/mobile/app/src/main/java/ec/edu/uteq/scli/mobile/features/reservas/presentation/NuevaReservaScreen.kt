package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun NuevaReservaScreen(viewModel: NuevaReservaViewModel) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("nueva_solicitud_form"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nueva solicitud", style = MaterialTheme.typography.headlineSmall)
        if (state.cargandoCatalogos) CircularProgressIndicator()
        Text("Docente: ${state.docenteCodigo.ifBlank { "Cargando…" }}")
        Selector("Materia", state.materias.map { it.id to "${it.codigo} — ${it.nombre}" }, state.materiaId) { id -> viewModel.actualizar { it.copy(materiaId = id) } }
        Text("Período: ${state.periodo?.let { "${it.codigo} — ${it.nombre}" } ?: "No disponible"}")
        Selector("Laboratorio", state.laboratorios.map { it.id to "${it.codigo} — ${it.nombre}" }, state.laboratorioId) { id -> viewModel.actualizar { it.copy(laboratorioId = id) } }
        Campo("Fecha (AAAA-MM-DD)", state.fechaReserva) { value -> viewModel.actualizar { it.copy(fechaReserva = value) } }
        Campo("Hora inicio (HH:mm)", state.horaInicio) { value -> viewModel.actualizar { it.copy(horaInicio = value) } }
        Campo("Hora fin (HH:mm)", state.horaFin) { value -> viewModel.actualizar { it.copy(horaFin = value) } }
        Campo("Participantes", state.numeroParticipantes) { value -> viewModel.actualizar { it.copy(numeroParticipantes = value) } }
        Campo("Motivo", state.motivo) { value -> viewModel.actualizar { it.copy(motivo = value) } }
        Campo("Observación (opcional)", state.observacion) { value -> viewModel.actualizar { it.copy(observacion = value) } }
        Button(onClick = viewModel::comprobarDisponibilidad, enabled = !state.comprobando && listOf(state.laboratorioId, state.fechaReserva, state.horaInicio, state.horaFin).none(String::isBlank), modifier = Modifier.fillMaxWidth().testTag("comprobar_disponibilidad")) { Text("Comprobar disponibilidad") }
        state.disponible?.let { Text(if (it) "Disponible" else "No disponible", modifier = Modifier.testTag("resultado_disponibilidad")) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("nueva_solicitud_error")) }
        state.solicitudCreada?.let { Text("Solicitud creada correctamente — ${it.estado}", modifier = Modifier.testTag("nueva_solicitud_exito")) }
        Button(onClick = viewModel::enviar, enabled = !state.enviando && !state.cargandoCatalogos, modifier = Modifier.fillMaxWidth().testTag("enviar_solicitud")) { Text(if (state.enviando) "Enviando…" else "Enviar solicitud") }
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
