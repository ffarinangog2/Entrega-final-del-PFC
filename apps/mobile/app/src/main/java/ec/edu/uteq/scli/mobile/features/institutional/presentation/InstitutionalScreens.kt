package ec.edu.uteq.scli.mobile.features.institutional.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlanificacionesScreen(viewModel: InstitutionalViewModel, puedeRevisar: Boolean, coordinador: Boolean) {
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
