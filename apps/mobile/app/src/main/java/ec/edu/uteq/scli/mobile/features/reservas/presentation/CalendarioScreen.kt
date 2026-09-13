package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosRepository
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.LaboratorioCatalogoDto

@Composable
fun CalendarioScreen(viewModel: ReservasViewModel, catalogos: CatalogosRepository) {
    val state by viewModel.uiState.collectAsState()
    var labs by remember { mutableStateOf<List<LaboratorioCatalogoDto>>(emptyList()) }
    LaunchedEffect(Unit) { runCatching { catalogos.laboratorios() }.onSuccess { labs = it } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calendario", style = MaterialTheme.typography.headlineSmall)
        Text("Agenda autorizada por el servidor")
        LazyColumn { items(state.reservas, key = { it.id }) { reserva ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) {
                Text(reserva.fechaReserva)
                Text("${reserva.horaInicio} - ${reserva.horaFin}")
                Text("Laboratorio: ${labs.firstOrNull { it.id == reserva.laboratorioId }?.let { "${it.codigo} — ${it.nombre}" } ?: "No disponible"}")
                Text(reserva.estado.replace('_', ' '))
            } }
        } }
    }
}
