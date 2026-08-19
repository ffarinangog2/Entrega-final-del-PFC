package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ReservaDetalleScreen(reservaId: String, viewModel: ReservasViewModel) {
    val state by viewModel.uiState.collectAsState()
    var mostrarCancelacion by remember { mutableStateOf(false) }
    LaunchedEffect(reservaId) { viewModel.cargarDetalle(reservaId) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).testTag("reserva_detalle"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            state.cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            state.error != null -> Text(requireNotNull(state.error))
            state.seleccionada != null -> {
                val reserva = state.seleccionada
                if (state.desdeCache) {
                    Text("Sin conexión: mostrando datos guardados")
                    state.errorActualizacion?.let { Text("No se pudo actualizar: $it") }
                }
                Text(reserva!!.codigoReserva)
                Text("Estado: ${reserva.estado}")
                Text("Laboratorio: ${reserva.laboratorioId}")
                Text("Responsable: ${reserva.responsableId}")
                Text("Fecha: ${reserva.fechaReserva}")
                Text("Horario: ${reserva.horaInicio} - ${reserva.horaFin}")
                Text("Solicitud: ${reserva.solicitudId}")
                Text("Creada: ${reserva.creadaEn}")
                Text("Actualizada: ${reserva.actualizadaEn}")
                if (reserva.estado == "PROGRAMADA") {
                    Button(
                        onClick = { mostrarCancelacion = true },
                        enabled = !state.cancelando,
                        modifier = Modifier.testTag("cancelar_reserva"),
                    ) { Text(if (state.cancelando) "Cancelando" else "Cancelar reserva") }
                }
            }
        }
    }

    if (mostrarCancelacion) {
        var motivo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarCancelacion = false },
            title = { Text("Cancelar reserva") },
            text = {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelarReserva(motivo)
                        mostrarCancelacion = false
                    },
                    enabled = motivo.isNotBlank(),
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCancelacion = false }) { Text("Volver") } },
        )
    }
}
