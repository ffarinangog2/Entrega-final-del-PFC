package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.features.institutional.presentation.InstitutionalViewModel
import ec.edu.uteq.scli.mobile.features.qr.util.QrCodeGenerator

@Composable
fun ReservaDetalleScreen(
    reservaId: String,
    viewModel: ReservasViewModel,
    institutionalViewModel: InstitutionalViewModel,
    puedeCancelar: Boolean = true,
    puedeGestionarAsistencia: Boolean = false,
) {
    val state by viewModel.uiState.collectAsState()
    val institutionalState by institutionalViewModel.uiState.collectAsState()
    var mostrarCancelacion by remember { mutableStateOf(false) }
    LaunchedEffect(reservaId) { viewModel.cargarDetalle(reservaId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("reserva_detalle"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            state.cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            state.error != null -> Text(requireNotNull(state.error), color = MaterialTheme.colorScheme.error)
            state.seleccionada != null -> {
                val reserva = state.seleccionada
                if (state.desdeCache) {
                    Text("Sin conexión: mostrando datos guardados", color = MaterialTheme.colorScheme.secondary)
                    state.errorActualizacion?.let { Text("No se pudo actualizar: $it", color = MaterialTheme.colorScheme.error) }
                }
                Text(
                    text = "Reserva: ${reserva!!.codigoReserva}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text("Estado: ${reserva.estado}", style = MaterialTheme.typography.titleMedium)
                Text("Laboratorio: ${reserva.laboratorioId}")
                Text("Responsable: ${reserva.responsableId}")
                Text("Fecha: ${reserva.fechaReserva}")
                Text("Horario: ${reserva.horaInicio} - ${reserva.horaFin}")
                if (reserva.solicitudId.isNotBlank()) {
                    Text("Solicitud asociada: ${reserva.solicitudId}")
                }
                Text("Creada: ${reserva.creadaEn}")
                Text("Actualizada: ${reserva.actualizadaEn}")

                if (reserva.estado == "PROGRAMADA" && puedeCancelar) {
                    Button(
                        onClick = { mostrarCancelacion = true },
                        enabled = !state.cancelando,
                        modifier = Modifier.testTag("cancelar_reserva"),
                    ) { Text(if (state.cancelando) "Cancelando…" else "Cancelar reserva") }
                }

                if (puedeGestionarAsistencia && reserva.estado == "PROGRAMADA") {
                    Button(onClick = { institutionalViewModel.iniciarReserva(reserva.id) { viewModel.cargarDetalle(reserva.id) } }) {
                        Text("Iniciar utilización")
                    }
                }

                if (puedeGestionarAsistencia && reserva.estado == "EN_CURSO") {
                    if (institutionalState.sesion == null) {
                        Button(onClick = { institutionalViewModel.abrirSesion(reserva.id) }) { Text("Abrir asistencia") }
                    } else {
                        Text("Sesión de asistencia: ${institutionalState.sesion?.estado}")
                        val token = institutionalState.sesion?.token
                        val sesionId = institutionalState.sesion?.id
                        if (!token.isNullOrBlank() && !sesionId.isNullOrBlank()) {
                            val payload = QrCodeGenerator.construirPayloadAsistencia(sesionId, token)
                            val qrBitmap = remember(payload) {
                                QrCodeGenerator.generarBitmap(payload, 220, 220)
                            }
                            qrBitmap?.let { bmp ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Código QR para registro de asistencia",
                                        modifier = Modifier.size(200.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Los asistentes deben escanear este código QR para registrar su ingreso.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        Text("Asistentes registrados: ${institutionalState.asistentes.size}")
                        for (asistente in institutionalState.asistentes) {
                            Text("• ${asistente.estudianteId.ifBlank { asistente.id }} — ${asistente.estado}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = institutionalViewModel::refrescarSesion) { Text("Actualizar lista") }
                            Button(onClick = institutionalViewModel::cerrarSesion) { Text("Cerrar asistencia") }
                        }
                    }
                    Button(onClick = { institutionalViewModel.finalizarReserva(reserva.id) { viewModel.cargarDetalle(reserva.id) } }) {
                        Text("Finalizar utilización")
                    }
                }
                institutionalState.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                institutionalState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
                    modifier = Modifier.testTag("confirmar_cancelacion"),
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCancelacion = false }) { Text("Volver") }
            },
        )
    }
}
