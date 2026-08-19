package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ReservasScreen(
    viewModel: ReservasViewModel,
    onReservaClick: (String) -> Unit,
    onNuevaReserva: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    ReservasContent(
        state = state,
        onRefresh = { viewModel.cargarReservas(esRefresco = true) },
        onRetry = { viewModel.cargarReservas() },
        onReservaClick = onReservaClick,
        onNuevaReserva = onNuevaReserva,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ReservasContent(
    state: ReservasUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onReservaClick: (String) -> Unit,
    onNuevaReserva: () -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(state.refrescando, onRefresh)
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaReserva, modifier = Modifier.testTag("nueva_reserva")) {
                Text("+")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
                .testTag("reservas_listado"),
        ) {
            when {
                state.cargando && state.reservas.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).testTag("reservas_loading"),
                )
                state.error != null && state.reservas.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).testTag("reservas_error"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(requireNotNull(state.error))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
                state.reservas.isEmpty() -> Text(
                    "No hay reservas",
                    modifier = Modifier.align(Alignment.Center).testTag("reservas_empty"),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.reservas, key = { it.id }) { reserva ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReservaClick(reserva.id) }
                                .padding(16.dp)
                                .testTag("reserva_${reserva.id}"),
                        ) {
                            Text(reserva.codigoReserva)
                            Text(reserva.fechaReserva)
                            Text("${reserva.horaInicio} - ${reserva.horaFin}")
                            Text(reserva.estado)
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = state.refrescando,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
