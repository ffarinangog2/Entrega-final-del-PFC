package ec.edu.uteq.scli.mobile.features.notifications.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ec.edu.uteq.scli.mobile.common.navigation.MobileNavigationAccess
import ec.edu.uteq.scli.mobile.features.notifications.data.NotificacionInternaResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatearFechaNotificacion(creadaEn: String): String {
    if (creadaEn.isBlank()) return ""
    return runCatching {
        val instant = Instant.parse(creadaEn)
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        formatter.format(instant.atZone(zone))
    }.getOrElse {
        creadaEn.take(19).replace("T", " ")
    }
}

@Composable
internal fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    access: MobileNavigationAccess? = null,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    val tieneNoLeidas = uiState.items.any { !it.leida }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Notificaciones",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Button(
                onClick = { viewModel.marcarTodas() },
                enabled = tieneNoLeidas && !uiState.loading,
            ) {
                Text("Marcar todas como leídas")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.loading && uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refrescar() }) {
                        Text("Reintentar")
                    }
                }
            }
        } else if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No tienes notificaciones.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    NotificationItemCard(
                        item = item,
                        onClick = {
                            viewModel.marcarLeida(item.id) {
                                if (access != null) {
                                    val destino = resolveNotificationDestination(
                                        tipo = item.tipo,
                                        referenciaId = item.referenciaId,
                                        access = access,
                                    )
                                    if (destino != null) {
                                        onNavigate(destino)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    item: NotificacionInternaResponse,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.leida) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.leida) 1.dp else 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.leida) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (!item.leida) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("Nueva", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Text(
                text = item.cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val fechaFormateada = formatearFechaNotificacion(item.creadaEn)
            if (fechaFormateada.isNotBlank()) {
                Text(
                    text = fechaFormateada,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
