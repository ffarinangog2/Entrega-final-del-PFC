package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun NuevaReservaScreen(viewModel: NuevaReservaViewModel) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("nueva_reserva_form"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Reserva rápida")
        Campo("Solicitante ID", state.solicitanteId) {
            viewModel.actualizarFormulario { state -> state.copy(solicitanteId = it) }
        }
        Campo("Docente ID", state.docenteId) {
            viewModel.actualizarFormulario { state -> state.copy(docenteId = it) }
        }
        Campo("Laboratorio ID", state.laboratorioId) {
            viewModel.actualizarFormulario { state -> state.copy(laboratorioId = it) }
        }
        Campo("Materia ID", state.materiaId) {
            viewModel.actualizarFormulario { state -> state.copy(materiaId = it) }
        }
        Campo("Periodo lectivo ID", state.periodoLectivoId) {
            viewModel.actualizarFormulario { state -> state.copy(periodoLectivoId = it) }
        }
        Campo("Fecha (AAAA-MM-DD)", state.fechaReserva) {
            viewModel.actualizarFormulario { state -> state.copy(fechaReserva = it) }
        }
        Campo("Hora inicio (HH:mm)", state.horaInicio) {
            viewModel.actualizarFormulario { state -> state.copy(horaInicio = it) }
        }
        Campo("Hora fin (HH:mm)", state.horaFin) {
            viewModel.actualizarFormulario { state -> state.copy(horaFin = it) }
        }
        Campo("Número de participantes", state.numeroParticipantes) {
            viewModel.actualizarFormulario { state -> state.copy(numeroParticipantes = it) }
        }
        Campo("Motivo", state.motivo) {
            viewModel.actualizarFormulario { state -> state.copy(motivo = it) }
        }
        Campo("Observación (opcional)", state.observacion) {
            viewModel.actualizarFormulario { state -> state.copy(observacion = it) }
        }

        state.error?.let { Text("Error: $it", modifier = Modifier.testTag("nueva_reserva_error")) }
        state.solicitudCreada?.let {
            Text("Solicitud creada: ${it.id}", modifier = Modifier.testTag("nueva_reserva_exito"))
            Text("Estado: ${it.estado}")
        }
        Button(
            onClick = viewModel::enviar,
            enabled = !state.enviando,
            modifier = Modifier.fillMaxWidth().testTag("enviar_reserva"),
        ) {
            if (state.enviando) CircularProgressIndicator()
            else Text("Enviar solicitud")
        }
    }
}

@Composable
private fun Campo(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
