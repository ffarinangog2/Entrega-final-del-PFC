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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ReservasScreen(
    viewModel: ReservasViewModel,
    onReservaClick: (String) -> Unit,
    onNuevaReserva: () -> Unit,
    onSolicitudClick: (String) -> Unit = {},
    puedeCrear: Boolean = true,
) {
    val state by viewModel.uiState.collectAsState()
    ReservasContent(
        state = state,
        onRefresh = { viewModel.cargarReservas(esRefresco = true) },
        onRetry = { viewModel.cargarReservas() },
        onReservaClick = onReservaClick,
        onNuevaReserva = onNuevaReserva,
        onSolicitudClick = onSolicitudClick,
        puedeCrear = puedeCrear,
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
    onSolicitudClick: (String) -> Unit = {},
    puedeCrear: Boolean = true,
) {
    var tab by remember { mutableStateOf(0) }
    val pullRefreshState = rememberPullRefreshState(state.refrescando, onRefresh)
    Scaffold(
        floatingActionButton = { if (puedeCrear) {
            FloatingActionButton(onClick = onNuevaReserva, modifier = Modifier.testTag("nueva_reserva")) {
                Text("+")
            }
        } },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
                .testTag("reservas_listado"),
        ) {
            Column(Modifier.fillMaxSize()) {
            TabRow(tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Solicitudes") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Reservas") })
            }
            Box(Modifier.fillMaxSize()) { when {
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
                tab == 0 && state.solicitudes.isEmpty() -> Text(
                    "No hay solicitudes",
                    modifier = Modifier.align(Alignment.Center).testTag("reservas_empty"),
                )
                tab == 0 -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.solicitudes, key = { it.id }) { solicitud ->
                        Column(Modifier.fillMaxWidth().clickable { onSolicitudClick(solicitud.id) }.padding(16.dp).testTag("solicitud_${solicitud.id}")) {
                            Text("Solicitud — ${solicitud.estado.replace('_', ' ')}")
                            Text(solicitud.fechaReserva)
                            Text("${solicitud.horaInicio} - ${solicitud.horaFin}")
                        }
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.desdeCache) {
                        item { Text("Sin conexión: mostrando reservas guardadas") }
                        state.errorActualizacion?.let { error ->
                            item { Text("No se pudo actualizar: $error") }
                        }
                    }
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
            } }
            PullRefreshIndicator(
                refreshing = state.refrescando,
                state = pullRefreshState,
                modifier = Modifier,
            )
            }
        }
    }
}
