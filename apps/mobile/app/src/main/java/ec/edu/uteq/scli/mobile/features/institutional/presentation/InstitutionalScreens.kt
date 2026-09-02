package ec.edu.uteq.scli.mobile.features.institutional.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.features.institutional.data.CoordinacionData
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionDto

@Composable
fun PlanificacionesScreen(viewModel: InstitutionalViewModel, puedeRevisar: Boolean, coordinador: Boolean) {
    if (coordinador) {
        CoordinacionScreen(viewModel)
        return
    }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarPlanificaciones() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Planificación semestral") }
        if (state.cargando) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it) } }
        state.mensaje?.let { item { Text(it) } }
        if (!state.cargando && state.planificaciones.isEmpty()) item { Text("No hay planificaciones para tu ámbito") }
        items(state.planificaciones, key = { it.id }) { plan ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${plan.diaSemana} ${plan.horaInicio}–${plan.horaFin}")
                Text("Estado: ${plan.estado.replace('_', ' ')}")
                plan.observacion?.let { Text(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (puedeRevisar && plan.estado == "ENVIADA") {
                        Button(onClick = { viewModel.aceptar(plan.id) }) { Text("Aceptar") }
                        OutlinedButton(onClick = { viewModel.rechazar(plan.id, "Rechazada desde Mobile") }) { Text("Rechazar") }
                    }
                    if (coordinador && plan.estado == "PROPUESTA_CAMBIO") {
                        Button(onClick = { viewModel.aceptarPropuesta(plan.id) }) { Text("Aceptar propuesta") }
                    }
                }
            }
        }
    }
}

private val diasPlanificacion = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")

@Composable
fun CoordinacionScreen(viewModel: InstitutionalViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarCoordinacion() }
    CoordinacionContent(state)
}

@Composable
internal fun CoordinacionContent(state: InstitutionalUiState) {
    var diaSeleccionado by remember { mutableStateOf("LUNES") }
    var mostrarLaboratorios by remember { mutableStateOf(false) }
    val data = state.coordinacion

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Coordinación", style = MaterialTheme.typography.headlineMedium)
            Text("Consulta y seguimiento de la planificación semestral")
        }
        if (state.cargando && data == null) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        data?.let { coordinacion ->
            item { ResumenCoordinacion(coordinacion) }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    diasPlanificacion.forEach { dia ->
                        FilterChip(
                            selected = diaSeleccionado == dia,
                            onClick = { diaSeleccionado = dia },
                            label = { Text(etiquetaDia(dia)) },
                        )
                    }
                }
            }
            val planesDia = coordinacion.planificaciones
                .filter { it.diaSemana == diaSeleccionado && it.estado != "CANCELADA" }
                .sortedBy { it.horaInicio }
            if (planesDia.isEmpty()) item { Text("No hay asignaciones para ${etiquetaDia(diaSeleccionado)}.") }
            items(planesDia, key = { it.id }) { plan ->
                AsignacionCard(plan, coordinacion)
            }
            item {
                OutlinedButton(onClick = { mostrarLaboratorios = !mostrarLaboratorios }) {
                    Text(if (mostrarLaboratorios) "Ocultar disponibilidad" else "Disponibilidad de laboratorios")
                }
            }
            if (mostrarLaboratorios) {
                items(coordinacion.laboratorios, key = { it.id }) { laboratorio ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(laboratorio.codigo, style = MaterialTheme.typography.titleMedium)
                            Text(laboratorio.nombre)
                            Text(estadoLaboratorio(laboratorio.estado))
                        }
                    }
                }
            }
            item {
                Text("Las notificaciones de revisión, propuestas y aprobación se reciben mediante las notificaciones de la aplicación.")
                Spacer(Modifier.height(4.dp))
                Text("La creación, edición y el envío completo se realizan principalmente desde la Web.")
            }
        }
    }
}

@Composable
private fun ResumenCoordinacion(data: CoordinacionData) {
    val carreraId = data.planificaciones.firstOrNull()?.carreraId ?: data.materias.firstOrNull()?.carreraId
    val carrera = data.carreras.find { it.id == carreraId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Carrera: ${carrera?.nombre ?: "Mi carrera institucional"}")
            Text("Periodo: ${data.periodo.codigo}")
            Text("Estado de planificación: ${estadoGeneral(data.planificaciones)}")
            Text("${data.planificaciones.count { it.estado != "CANCELADA" }} asignaciones")
        }
    }
}

@Composable
private fun AsignacionCard(plan: PlanificacionDto, data: CoordinacionData) {
    val materia = data.materias.find { it.id == plan.materiaId }
    val docente = data.docentes.find { it.id == plan.docenteId }
    val laboratorio = data.laboratorios.find { it.id == plan.laboratorioId }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${plan.horaInicio} - ${plan.horaFin}", style = MaterialTheme.typography.titleMedium)
            Text(materia?.nombre ?: "Materia no disponible")
            Text("Docente: ${docente?.codigoDocente ?: "Asignado"}")
            Text(laboratorio?.codigo ?: "Laboratorio no disponible")
            Text("Estado: ${etiquetaEstado(plan.estado)}")
            plan.observacion?.takeIf { it.isNotBlank() }?.let {
                Text("Observación: $it", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

internal fun estadoGeneral(planes: List<PlanificacionDto>): String {
    val vigentes = planes.filter { it.estado != "CANCELADA" }
    return when {
        vigentes.any { it.estado == "PROPUESTA_CAMBIO" } -> "DEVUELTA CON OBSERVACIONES"
        vigentes.any { it.estado == "BORRADOR" } -> "BORRADOR"
        vigentes.isNotEmpty() && vigentes.all { it.estado == "CONFIRMADA" } -> "APROBADA"
        vigentes.any { it.estado == "ENVIADA" } -> "EN REVISIÓN"
        vigentes.any { it.estado == "RECHAZADA" } -> "RECHAZADA"
        else -> "SIN INICIAR"
    }
}

internal fun etiquetaEstado(estado: String): String = when (estado) {
    "ENVIADA" -> "En revisión"
    "PROPUESTA_CAMBIO" -> "Devuelta con observaciones"
    "CONFIRMADA" -> "Aprobada"
    "BORRADOR" -> "Borrador"
    "RECHAZADA" -> "Rechazada"
    "CANCELADA" -> "Cancelada"
    else -> estado.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun etiquetaDia(dia: String): String =
    if (dia == "MIERCOLES") "Miércoles" else dia.lowercase().replaceFirstChar(Char::uppercase)

private fun estadoLaboratorio(estado: String): String = when (estado) {
    "DISPONIBLE" -> "Disponible"
    "OCUPADO" -> "Ocupado"
    "MANTENIMIENTO" -> "En mantenimiento"
    else -> estado.lowercase().replaceFirstChar(Char::uppercase)
}

@Composable
fun HistorialAsistenciaScreen(viewModel: InstitutionalViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarHistorial() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mi asistencia") }
        if (state.cargando) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it) } }
        state.mensaje?.let { item { Text(it) } }
        if (!state.cargando && state.historial.isEmpty()) item { Text("Todavía no hay registros") }
        items(state.historial, key = { it.id }) { registro ->
            Column { Text(registro.registradaEn); Text(registro.estado) }
        }
    }
}
