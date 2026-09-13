package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.features.auth.data.AuthUserResponse
import ec.edu.uteq.scli.mobile.features.auth.data.hasPermission
import ec.edu.uteq.scli.mobile.features.auth.data.hasRole
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosRepository
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.LaboratorioCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.PropuestaAlternativa

@Composable
fun SolicitudDetalleScreen(id: String, viewModel: ReservasViewModel, user: AuthUserResponse, catalogos: CatalogosRepository) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(id) { viewModel.cargarSolicitud(id) }
    var laboratorios by remember { mutableStateOf<List<LaboratorioCatalogoDto>>(emptyList()) }
    LaunchedEffect(Unit) { runCatching { catalogos.laboratorios() }.onSuccess { laboratorios = it } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.cargando) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.solicitudSeleccionada?.let { solicitud ->
            Text("Solicitud", style = MaterialTheme.typography.headlineSmall)
            Text("Estado: ${solicitud.estado.replace('_', ' ')}")
            Text("Fecha: ${solicitud.fechaReserva}")
            Text("Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}")
            Text("Participantes: ${solicitud.numeroParticipantes}")
            Text("Motivo: ${solicitud.motivo}")
            solicitud.observacion?.let { Text("Observación: $it") }
            if (solicitud.estado == "PROPUESTA") {
                Text("Propuesta alternativa", style = MaterialTheme.typography.titleMedium)
                Text("Fecha: ${solicitud.propuestaFecha}")
                Text("Horario: ${solicitud.propuestaHoraInicio} - ${solicitud.propuestaHoraFin}")
                solicitud.propuestaObservacion?.let { Text(it) }
                if (user.hasRole("DOCENTE") && solicitud.solicitanteId == user.perfilId) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ viewModel.actuarSolicitud("aceptar", user.perfilId) }) { Text("Aceptar propuesta") }
                        OutlinedButton({ viewModel.actuarSolicitud("rechazar_propuesta", user.perfilId) }) { Text("Rechazar propuesta") }
                    }
                }
            }
            if (solicitud.estado == "PENDIENTE" && user.hasPermission("SOLICITUD_APROBAR"))
                Button({ viewModel.actuarSolicitud("revision", user.perfilId) }) { Text("Poner en revisión") }
            if (solicitud.estado == "EN_REVISION" && user.hasPermission("SOLICITUD_APROBAR"))
                Button({ viewModel.actuarSolicitud("aprobar", user.perfilId) }) { Text("Aprobar") }
            if (solicitud.estado == "EN_REVISION" && user.hasPermission("SOLICITUD_RECHAZAR"))
                OutlinedButton({ viewModel.actuarSolicitud("rechazar", user.perfilId, "Rechazada desde Mobile") }) { Text("Rechazar") }
            if (solicitud.estado == "EN_REVISION" && user.hasPermission("SOLICITUD_APROBAR")) {
                var expanded by remember { mutableStateOf(false) }
                var laboratorioId by remember { mutableStateOf("") }
                var fecha by remember { mutableStateOf("") }
                var inicio by remember { mutableStateOf("") }
                var fin by remember { mutableStateOf("") }
                var observacion by remember { mutableStateOf("") }
                Text("Proponer alternativa", style = MaterialTheme.typography.titleMedium)
                Box { OutlinedButton({ expanded = true }) { Text(laboratorios.firstOrNull { it.id == laboratorioId }?.let { "${it.codigo} — ${it.nombre}" } ?: "Seleccionar laboratorio") }; DropdownMenu(expanded, { expanded = false }) { laboratorios.forEach { lab -> DropdownMenuItem({ Text("${lab.codigo} — ${lab.nombre}") }, { laboratorioId = lab.id; expanded = false }) } } }
                OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha") })
                OutlinedTextField(inicio, { inicio = it }, label = { Text("Hora inicio") })
                OutlinedTextField(fin, { fin = it }, label = { Text("Hora fin") })
                OutlinedTextField(observacion, { observacion = it }, label = { Text("Observación") })
                Button({ viewModel.actuarSolicitud("proponer", user.perfilId, propuesta = PropuestaAlternativa(laboratorioId, fecha, inicio, fin, observacion.ifBlank { null })) }, enabled = laboratorioId.isNotBlank() && fecha.isNotBlank() && inicio.isNotBlank() && fin.isNotBlank()) { Text("Enviar propuesta") }
            }
            if (solicitud.solicitanteId == user.perfilId && solicitud.estado in setOf("PENDIENTE", "EN_REVISION", "PROPUESTA", "APROBADA"))
                OutlinedButton({ viewModel.actuarSolicitud("cancelar", user.perfilId, "Retirada desde Mobile") }) { Text("Cancelar/Retirar") }
            Text("Historial", style = MaterialTheme.typography.titleMedium)
            state.historial.forEach { Text("${it.estadoAnterior ?: "INICIO"} → ${it.estadoNuevo}${it.comentario?.let { c -> ": $c" } ?: ""}") }
        }
    }
}
